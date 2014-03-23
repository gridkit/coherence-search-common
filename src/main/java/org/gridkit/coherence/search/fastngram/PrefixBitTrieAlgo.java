/**
 * Copyright 2011-2014 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.coherence.search.fastngram;


abstract class PrefixBitTrieAlgo {

    protected final int BLANK = 255;
    protected final int STAGE_ROLL = 4;
    protected final long PLET_MASK = 0x0F;
    
    /** @return mask for valid token */
    protected abstract long tokenMask();

    /** @return mask for address part of token */
    protected abstract long addressMask();

    /** @return mask for value part of token */
    protected abstract long valueMask();
    
    /** @return depth of radix tree */
    protected abstract int treeDepth();

    /** 
     * Allocate new node and assign it to specified position of node page:slot.
     * <br/>
     * Node page:slot may be relocated if its target page doesn't have capacity for extra node. 
     * 
     * @param page
     * @param slot
     * @param pos
     * @return slot (in page address) of new node
     */
    protected abstract int addSlot(int page, int slot, int pos);
    
    /** Mark slot as free */
    protected abstract void freeSlot(int page, int slot, boolean verify);

    /** Calculates "stage roll address" from token*/
    protected abstract long stageAddress(long token, int stage);

    /** Get target page for node */
    protected abstract int getPage(int page, int slot);

    /** Copy node from srcPage:srcSlot to dstPage:dstSlot */
    protected abstract void nodeCopy(int srcPage, int srcSlot, int dstPage, int dstSlot);

    /** @return true if page:slot is blank */
    protected abstract boolean nodeIsBlank(int page, int slot);

    /** Blanks all positions in node page:slot */
    protected abstract void nodeBlank(int page, int slot);

    /** @return max non-blank position for node page:slot below or equal of limit specified */
    protected abstract int nodeMaxIndex(int page, int slot, int max);

    /** @return min non-blank position for node page:slot above or equal of limit specified */
    protected abstract int nodeMinIndex(int page, int slot, int min);

    /** @return read specified position from node */
    protected abstract int nodeGet(int page, int slot, int pos);

    /** Sets specified position of node */
    protected abstract void nodeSet(int page, int slot, int pos, int val8);

    /** @return number of non-blank positions */
    protected abstract int nodePositionsUsed(int page, int slot);

    /** @return whene node is marked as occupied */
    protected abstract boolean isMarkedSlot(int page, int slot);

    protected abstract void incSize();

    protected abstract void decSize();
    
    protected long to64(int i) {
        return 0xFFFFFFFFl & ((long) i);
    }

    protected int to32(long i) {
        return (int) i;
    }

    protected long internalGet(long token) {
        if (token != (tokenMask() & token)) {
            throw new IllegalArgumentException("Token out of mask " + Long.toHexString(tokenMask()) + " " + token);
        }
        long addr = stageAddress(token, 0);
        int cpage = 0; // root
        int cslot = 0; // root
        int stageLimit = treeDepth() - 1;
        for(int stage = 0; stage != stageLimit; ++stage) {
            addr = Long.rotateLeft(addr, STAGE_ROLL);
            int nplet = (int)(PLET_MASK & addr);
            int ref = nodeGet(cpage, cslot, nplet);
            if (ref == BLANK) {
                return ~token;
            }
            cpage = getPage(cpage, cslot);
            cslot = ref;
        }
        addr = Long.rotateLeft(addr, STAGE_ROLL);
        int nplet = (int)(PLET_MASK & addr);
        int v = nodeGet(cpage, cslot, nplet);
        if (v == BLANK) {
            return ~token;
        }
        return (token & ~valueMask()) | (valueMask() & v);
    }
    
    protected long internalPut(long value, boolean getPrev) {
        if (value != (tokenMask() & value)) {
            throw new IllegalArgumentException("Value out of mask " + Long.toHexString(tokenMask()) + " " + value);
        }
        int v = to32(valueMask() & value);
        long addr = stageAddress(value, 0);
        int cpage = 0; // root
        int cslot = 0; // root
        int stageLimit = treeDepth() - 1;
        for (int stage = 0; stage != stageLimit; ++stage) {
            addr = Long.rotateLeft(addr, STAGE_ROLL);
            int nplet = (int) (PLET_MASK & addr);
            int ref = nodeGet(cpage, cslot, nplet);
            if (ref == 255) {
                ref = addSlot(cpage, cslot, nplet);
            }
            cpage = getPage(cpage, cslot);
            cslot = ref;
        }
        addr = Long.rotateLeft(addr, STAGE_ROLL);
        int nplet = (int) (PLET_MASK & addr);
        int val = nodeGet(cpage, cslot, nplet);
        nodeSet(cpage, cslot, nplet, v);
        if (val == BLANK) {
            incSize();
            return !getPrev ? 0 : ~value;
        } else {
            return !getPrev ? 0 : (value & addressMask()) | (valueMask() & val);
        }
    }

    protected long internalRemove(long token, boolean getPrev) {
        if (token != (tokenMask() & token)) {
            throw new IllegalArgumentException("Value out of mask " + Long.toHexString(tokenMask()) + " " + token);
        }
        long addr = stageAddress(token, 0);
        // top most single child node
        int slevel = -1;
        int spage = 0;
        int sslot = 0;
        int cpage = 0; // root
        int cslot = 0; // root
        int lpage = 0; // last page
        int lslot = 0; // last slot
        int stageLimit = treeDepth() - 1;
        for (int stage = 0; stage != stageLimit; ++stage) {
            addr = Long.rotateLeft(addr, STAGE_ROLL);
            int nplet = (int) (PLET_MASK & addr);
            int ref = nodeGet(cpage, cslot, nplet);
            if (ref == BLANK) {
                if (getPrev) {
                    return ~token;
                } else {
                    return 0;
                }
            }
            lpage = cpage;
            lslot = cslot;
            cpage = getPage(cpage, cslot);
            cslot = ref;
            // remember start begining of thin branch
            if (nodePositionsUsed(cpage, cslot) == 1) {
                if (slevel < 0) {
                    slevel = stage;
                    spage = lpage;
                    sslot = lslot;
                }
            } else {
                slevel = -1;
            }
        }

        addr = Long.rotateLeft(addr, STAGE_ROLL);
        int nplet = (int) (PLET_MASK & addr);
        int val = nodeGet(cpage, cslot, nplet);
        if (val == BLANK) {
            return getPrev ? ~token : 0;
        } else {
            decSize();
            nodeSet(cpage, cslot, nplet, BLANK);
        }

        if (slevel >= 0) {
            // removing singleton branch
            cpage = spage;
            cslot = sslot;
            addr = stageAddress(token, slevel + 1);
            nplet = (int) (PLET_MASK & addr);
            int sref = nodeGet(spage, sslot, nplet);
            nodeSet(spage, sslot, nplet, BLANK);
            cpage = getPage(spage, sslot);
            cslot = sref;
            for (int l = slevel + 1; l != treeDepth(); ++l) {
                addr = Long.rotateLeft(addr, STAGE_ROLL);
                nplet = (int) (PLET_MASK & addr);
                int ref = nodeGet(cpage, cslot, nplet);
                nodeSet(cpage, cslot, nplet, BLANK);
                freeSlot(cpage, cslot, true);
                cpage = getPage(cpage, cslot);
                cslot = ref;
            }
        }

        if (getPrev) {
            return (token & addressMask()) | (valueMask() & val);
        } else {
            return 0;
        }
    }

    protected TextTree dumpTree() {
        return dumpTree(treeDepth() - 1, 0, 0);
    }

    TextTree dumpTree(int l, int page, int slot) {
        if (!isMarkedSlot(page, slot)) {
            System.err.println("Dangling pointer " + page + ":" + slot);
        }
        if (treeDepth() - l > 1 && page == 0 && slot == 0) {
            System.err.println("Pointer to root");
            return TextTree.t("NULL");
        }
        int c = nodePositionsUsed(page, slot);
        TextTree[] sub = new TextTree[c];
        if (l > 0) {
            int n = 0;
            int cpage = getPage(page, slot);
            for (int i = 0; i != 16; ++i) {
                int ref = nodeGet(page, slot, i);
                if (ref != 255) {
                    sub[n++] = TextTree.t("[" + Integer.toHexString(i) + "]", dumpTree(l - 1, cpage, ref));
                }
            }
        } else {
            int n = 0;
            for (int i = 0; i != 16; ++i) {
                int ref = nodeGet(page, slot, i);
                if (ref != 255) {
                    sub[n++] = TextTree.t("[" + Integer.toHexString(i) + "] = " + ref);
                }
            }
        }
        return TextTree.t((treeDepth() - l - 1) + "|" + page + ":" + slot + "", sub);
    }
}
