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

import java.util.Arrays;

/**
 * Radix tree based collection.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class PrefixBitTrie extends PrefixBitTrieAlgo {

    protected int[] pageHeap;
    protected int[] pageHeapIndex;

    // tree entries
    protected int[] pageRef; // page offset
    protected long[] tree;
    
    protected final int addressBits;
    protected final int dataBits;
    
    protected final int pageSize = 256;
    
    protected final long tokenMask;
    protected final long addressMask;
    protected final long valueMask; 
    
    /** Length of address in radix tree levels */
    protected final int depth;
    /** Number of highest bits to dispose in address word */ 
    protected final int addressShift;
    
    protected long size = 0;
    protected int slotUsed = 0;
    
    public PrefixBitTrie(int addressBits, int dataBits, int pageCapacity) {
        this.addressBits = addressBits;
        this.dataBits = dataBits;
        checkRange(addressBits, 1, 64, "addresBits (%d) out of [1, 64] range");
        checkRange(dataBits, 0, 7, "dataBits (%d) out of [0, 7] range");
        checkRange(addressBits + dataBits, 0, 64, "addressBits + dataBits (%d) out of [1, 64] range");
        checkRange(pageCapacity, 1, 1 << 30, "pageCapacity (%d) out of [1, 1 << 30] range");

        tokenMask = (-1l) >>> (64 - addressBits - dataBits);
        valueMask = dataBits == 0 ? 0 : (-1l) >>> (64 - dataBits);
        addressMask = tokenMask & ~valueMask;
        
        depth = (addressBits + 3) / 4;
        addressShift = 64 - 4 * depth - dataBits;
        
        // treeCapacity should power of 2
        if (pageCapacity != Integer.highestOneBit(pageCapacity)) {
            pageCapacity = Integer.highestOneBit(pageCapacity) << 1;
        }
        initTree(pageCapacity);
    }
    
    protected void checkRange(int v, int min, int max, String message) {
        if (v < min || v > max) {
            throw new IllegalArgumentException(String.format(message, v));
        }
    }
    
    @Override
    protected long tokenMask() {
        return tokenMask;
    }

    @Override
    protected long addressMask() {
        return addressMask;
    }

    @Override
    protected int valueBits() {
        return dataBits;
    }

    @Override
    protected long valueMask() {
        return valueMask;
    }

    @Override
    protected int treeDepth() {
        return depth;
    }

    protected void initTree(int pages) {
        pageHeap = new int[pages]; 
        pageHeapIndex = new int[pages]; 
        pageRef = new int[pageSize * pages];
        tree = new long[2 * pageSize * pages];
        initFreeSlotMap(0, pages);
        initPageHeap();
        markSlot(0, 0, true);
        nodeBlank(0, 0);
        ++slotUsed;
    }

    // free memory management
    
    protected void initFreeSlotMap(int n, int pages) {
        for(int i = n; i != pages; ++i) {
            // 2 slots (4 longs) are used for allocation bit map
            markSlot(i, 254, true);
            markSlot(i, 255, true);
        }
    }

    protected void initPageHeap() {
        for(int i = 0; i != pageHeap.length; ++i) {
            pageHeap[i] = i;
            pageHeapIndex[i] = i;
        }
    }

    protected void extendCapacity() {
        extendTree();
        extendPageRef();
        extendPageHeap();
//      assert fhValidate();
        fhValidate();
    }
    
    protected void extendTree() {
        tree = Arrays.copyOf(tree, 2 * tree.length);
        int p = tree.length / pageSize / 2;
        initFreeSlotMap(p / 2, p);
    }

    protected void extendPageRef() {
        pageRef = Arrays.copyOf(pageRef, 2 * pageRef.length);
    }
    
    protected void extendPageHeap() {
        int shift = pageHeap.length;
        int[] nHeap = new int[2 * pageHeap.length];
        int[] nHeapIndex = new int[2 * pageHeap.length];
        System.arraycopy(pageHeap, 0, nHeap, pageHeap.length, pageHeap.length);
        for(int i = 0; i != pageHeap.length; ++i) {
            nHeap[i] = shift + i;
            nHeapIndex[shift + i] = i;
            nHeapIndex[i] = pageHeapIndex[i] + shift;
        }
        pageHeap = nHeap;
        pageHeapIndex = nHeapIndex;
    }
    
    protected int findFreeSlot(int page) {
        long m0 = node0(page, 254);
        long m1 = node1(page, 254);
        long m2 = node0(page, 255);
        long m3 = node1(page, 255);
        
        int b;
        b = findBit(m0);
        if (b >= 0) {
            return b;
        }
        b = findBit(m1);
        if (b >= 0) {
            return 64 + b;
        }
        b = findBit(m2);
        if (b >= 0) {
            return 128 + b;
        }
        b = findBit(m3);
        if (b >= 0) {
            return 192 + b;
        }
        return -1;
    }
    
    protected int allocSlot(int page) {
        int slot = findFreeSlot(page);
        if (slot < 0) {
            throw new RuntimeException("Page is full");
        }
        markSlot(page, slot, true);
        nodeBlank(page, slot);
        return slot;
    }
    
    protected void updatePageRank(int page) {
        int n = pageHeapIndex[page];
        if (n == 0) {
            // ignore
        }
        else {
            fhUpdateRank(0);
            fhUpdateRank(n);
        }
    }
    
    protected int findBit(long word) {
        long b = Long.lowestOneBit(~word);
        if (b == 0) {
            return -1;
        }
        else {
            return Long.numberOfTrailingZeros(b);
        }
    }
    
    protected void markSlot(int page, int slot, boolean occupied) {
        int off = 508 + (slot / 64);
        assert off >= 508;
        assert off < 512;
        long bit = 1l << (slot % 64);
        if (occupied) {
            tree[2 * page * pageSize + off] |= bit;
        }
        else {
            tree[2 * page * pageSize + off] &= ~bit;            
        }
    }
    
    @Override
    protected boolean isMarkedSlot(int page, int slot) {
        int off = 508 + (slot / 64);
        assert off >= 508;
        assert off < 512;
        long bit = 1l << (slot % 64);
        return 0 != (tree[2 * page * pageSize + off] & bit);
    }
    
    /** @return free slots for page */
    protected int free(int page) {
        return 
                Long.bitCount(~node0(page, 254))
            +   Long.bitCount(~node1(page, 254))
            +   Long.bitCount(~node0(page, 255))
            +   Long.bitCount(~node1(page, 255));
    }

    protected long fhRank(int page) {
        return to64(free(page)) << 32 | to64(-page);
    }
    
    protected void fhUpdateRank(int n) {
        while(true) {
            int cpage = pageHeap[n];
            long r = fhRank(cpage);
            if (n > 0) {
                int p = fhPar(n);
                if (r > fhRank(pageHeap[p])) {
                    fhPushUp(n);
                    continue;
                }
            }
            int c1 = fhLeft(n);
            if (c1 < pageHeap.length && r < fhRank(pageHeap[c1])) {
                if (fhPushDown(n) != n) {
                    continue;
                }
            }
            int c2 = fhRight(n);
            if (c2 < pageHeap.length && r < fhRank(pageHeap[c2])) {
                if (fhPushDown(n) != n) {
                    continue;
                }
            }
            break;
        }
    }

    protected void fhSet(int n, int page) {
        pageHeap[n] = page;
        pageHeapIndex[page] = n;
    }
    
    protected void fhSwap(int from, int to) {
        int opage = pageHeap[to];
        int npage = pageHeap[from];
        pageHeap[to] = npage;
        pageHeapIndex[npage] = to;
        pageHeap[from] = opage;
        pageHeapIndex[opage] = from;
    }
    
    /**
     * Pushes updated element downwards the heap
     */
    protected int fhPushDown(int n) {
        // tail recursion
        while(true) {
            int c1 = fhLeft(n);
            int c2 = fhRight(n);
            if (c2 >= pageHeap.length) {
                if (c1 >= pageHeap.length) {
                    return n;
                }
                else {
                    long cr = fhRank(pageHeap[n]);
                    long r1 = fhRank(pageHeap[c1]);
                    if (r1 > cr) {
                        // push down
                        fhSwap(c1, n);
                        n = c1;
                        continue;
                    }
                    else {
                        return n;
                    }
                }
            }
            long cr = fhRank(pageHeap[n]);
            long r1 = fhRank(pageHeap[c1]);
            if (r1 > cr) {
                // push down
                fhSwap(c1, n);
                n = c1;
                continue;
            }
            long r2 = fhRank(pageHeap[c2]);
            if (r2 > cr) {
                // push down
                fhSwap(c2, n);
                n = c2;
                continue;
            }
            return n;
        }
    }

    /**
     * Push updated element in the heap upwards
     */
    protected void fhPushUp(int n) {
        int i = n;
        long r = fhRank(pageHeap[i]);
        while(i > 0) {
            int p = fhPar(i);
            if (fhRank(pageHeap[p]) < r) {
                fhSwap(i, p);
                i = p;
            }
            else {
                break;
            }
        }
    }
    
    protected int fhPar(int n) {
        return (n - 1) >>> 1;
    }

    protected int fhLeft(int n) {
        return (n << 1) + 1;
    }

    protected int fhRight(int n) {
        return (n << 1) + 2;
    }
    
    protected boolean fhValidate() {
        for(int i = 0; i != pageHeap.length; ++i) {
            if (i != pageHeapIndex[pageHeap[i]]) {
                throw new AssertionError("Page heap linkage violation");
            }
            if (i > 0) {
                long r = fhRank(pageHeap[i]);
                int p = fhPar(i);
                long pr = fhRank(pageHeap[p]);
                if (r >= pr) {
                    throw new AssertionError("Heap property violation: " + p + ":" + free(p) + "  " + i + ":" + free(i));
                }
            }
        }
        return true;
    }
    
    // memory access
    
    protected long node0(int page, int offs) {
        return tree[2 * (page * pageSize + offs)];
    }

    protected long node1(int page, int offs) {
        return tree[2 * (page * pageSize + offs) + 1];
    }

    @Override
    protected int nodeOccupancy(int page, int slot) {
        // I do not use loop in hope of global expression optimization
        // after inlining
        return 
                (nodeGet(page, slot, 0) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 1) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 2) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 3) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 4) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 5) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 6) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 7) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 8) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 9) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 10) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 11) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 12) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 13) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 14) != 255 ? 1 : 0)
            +   (nodeGet(page, slot, 15) != 255 ? 1 : 0);
    }

    @Override
    protected int nodeGet(int page, int slot, int idx) {
        int i = 2 * (page * pageSize + slot) + (idx < 8 ? 0 : 1);
        int r = 8 * (idx & 7);
        return (int)(0xFF & (tree[i] >> r));
    }
    
    @Override
    protected void nodeSet(int page, int slot, int idx, int val8) {
        int i = 2 * (page * pageSize + slot) + (idx < 8 ? 0 : 1);
        int r = 8 * (idx & 7);
        long v = (0xFFl & val8) << r;
        long m = ~(0xFFl << r);
        tree[i] = (tree[i] & m) | v;
    }

    @Override
    protected int nodeMinIndex(int page, int slot, int min) {
        for(int i = min; i < 16; ++i) {
            if (nodeGet(page, slot, i) != 255) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected int nodeMaxIndex(int page, int slot, int max) {
        for(int i = max; i >= 0; --i) {
            if (nodeGet(page, slot, i) != 255) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    protected void nodeBlank(int page, int slot) {
        int i = 2 * (page * pageSize + slot);
        tree[i] = -1;
        tree[i + 1] = -1;
    }

    @Override
    protected boolean nodeIsBlank(int page, int slot) {
        int i = 2 * (page * pageSize + slot);
        return tree[i] == -1 && tree[i + 1] == -1;
    }

    @Override
    protected void nodeCopy(int srcPage, int srcSlot, int dstPage, int dstSlot) {
        int src = 2 * (srcPage * pageSize + srcSlot);
        int dst = 2 * (dstPage * pageSize + dstSlot);
        tree[dst] = tree[src];
        tree[dst + 1] = tree[src + 1];
        setPage(dstPage, dstSlot, getPage(srcPage, srcSlot));
    }

    @Override
    protected int getPage(int page, int slot) {
        int i = page * pageSize + slot;
        return pageRef[i];
    }

    protected void setPage(int page, int slot, int refPage) {
        int i = page * pageSize + slot;
        pageRef[i] = refPage;
    }

    @Override
    protected void incSize() {
        ++size;
    }

    @Override
    protected void decSize() {
        --size;
    }

    public long size() {
        return size;
    }
    
    protected int slotUsed() {
        return slotUsed;
    }

    public long get(long token) {
        return internalGet(token);
    }

    /**
     * Seek for this or min key greater than this.
     * @param token key-only token
     * @return token composed for key and value, ~token if not found
     */
    public long getCeil(long token) {
        return internalGetOrNext(token, true);
    }

    /**
     * Seek for this or max key lower than this.
     * @param token key-only token
     * @return token composed for key and value, ~token if not found
     */
    public long getFloor(long token) {
        return internalGetOrNext(token, false);
    }
    
    public void put(long value) {
        internalPut(value, false);
    }

    public long getAndPut(long value) {
        return internalPut(value, true);
    }
    
    public void remove(long token) {
        internalRemove(token, false);
    }

    public long getAndRemove(long token) {
        return internalRemove(token, true);
    }
    
    @Override
    protected long stageAddress(long token, int stage) {
        return Long.rotateLeft(token & addressMask, addressShift + 4 * stage);
    }
    
    @Override
    protected int addSlot(int page, int slot, int f) {
        int hpage = getPage(page, slot);
        if (free(hpage) > 0) {
            int n = allocSlot(hpage);
            ++slotUsed;
            nodeSet(page, slot, f, n);
            updatePageRank(hpage);
            return n;
        }
        else {
            int refCount = nodeOccupancy(page, slot);
            int npage = findHostPage(refCount + 1);
            setPage(page, slot, npage);
            int nslot = -1;
            for(int i = 0; i != 16; ++i) {
                int ref = nodeGet(page, slot, i);
                if (i == f) {
                    assert ref == 255;
                    nslot = allocSlot(npage);
                    ++slotUsed;
                    nodeSet(page, slot, f, nslot);                  
                }
                else if (ref != 255) {
                    int n = allocSlot(npage);
                    nodeSet(page, slot, i, n);                  
                    nodeCopy(hpage, ref, npage, n);
                    markSlot(hpage, ref, false);
                }
            }
            updatePageRank(hpage);
            updatePageRank(npage);
            return nslot;
        }
    }

    @Override
    protected void freeSlot(int page, int slot, boolean verify) {
        if (verify) {
            assert !nodeIsBlank(page, slot);
        }
        markSlot(page, slot, false);
        --slotUsed;
        updatePageRank(page);
    }
    
    protected int findHostPage(int size) {
        fhUpdateRank(0);
        int tp = pageHeap[0];
        if (free(tp) >= size) {
            return tp;
        }
        else {
            // largest free page cannot accommodate node move
            extendCapacity();
            return pageHeap[0];
        }
    }
}
