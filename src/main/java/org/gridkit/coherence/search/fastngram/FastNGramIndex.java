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

public class FastNGramIndex extends AbstractNGramIndex {

    final int[][] lookupTable;
    
    public FastNGramIndex(int ngramSize, int bitsPerChar) {
        super(ngramSize, bitsPerChar);
        int termBits = bitsPerChar * ngramSize;
        if (termBits > 24) {
            throw new IllegalArgumentException("Term length " + termBits + " bits is above 24 bit limit");
        }
        lookupTable = new int[1 << termBits][];
        
    }
    
    @Override
    protected int seekTerm(int term) {
        int t = term;
        while(t < lookupTable.length) {
            if (lookupTable[t] != null) {
                return t;
            }
            ++t;
        }
        return -1;
    }

    @Override
    protected int seekTermRow(int term, int rowId) {
        int[] refs = lookupTable[term];
        if (refs != null) {
            return seekRow(refs, rowId);
        }
        else {
            return -1;
        }
    }

    @Override
    protected boolean imapContains(int term, int row) {
        int[] refs = lookupTable[term];
        if (refs != null) {
            return row == seekRow(refs, row);
        }
        else {
            return false;
        }
    }

    @Override
    protected void imapAdd(int term, int row) {
        if (lookupTable[term] == null) {
            lookupTable[term] = new int[]{1, row, 0, 0};
        }
        else {
            int[] rl = lookupTable[term];
            int size = rl[0];
            int n = Arrays.binarySearch(rl, 1, size + 1, row);
            if (n < 0) {
                // row is not added yet
                int p = ~n;
                
                if (size + 1 >= rl.length) {
                    // grow array
                    rl = grow(rl);
                    lookupTable[term] = rl;
                }
                for(int i = size; i >= p; --i) {
                    rl[i + 1] = rl[i];
                }
                rl[p] = row;
                rl[0]++;
            }
        }
    }

    @Override
    protected void imapRemove(int term, int row) {
        if (lookupTable[term] != null) {
            int[] rl = lookupTable[term];
            int size = rl[0];
            int n = Arrays.binarySearch(rl, 1, size + 1, row);
            if (n > 0) {
                for(int i = n; i < size; ++i) {
                    rl[i] = rl[i + 1];
                }
                rl[0]--;
                if (rl[0] == 0) {
                    lookupTable[term] = null;
                }
                else {
                    int[] nrl = shrink(rl);
                    if (nrl != rl) {
                        lookupTable[term] = nrl;
                    }
                }
            }
        }
    }

    private int seekRow(int[] refs, int row) {
        int n = Arrays.binarySearch(refs, 1, refs[0] + 1, row);
        if (n >= 0) {
            return refs[n];
        }
        else {
            int p = ~n;
            if (p <= refs[0]) {
                return refs[p];
            }
            else {
                return -1;
            }
        }
    }
    
    private int[] grow(int[] rl) {
        int step = rl.length >> 8;
        if (step < 4) {
            step = 4;
        }
        return Arrays.copyOf(rl, rl.length + step);
    }

    private int[] shrink(int[] rl) {
        int step = rl.length >> 8;
        if (step < 4) {
            step = 4;
        }
        int size = rl[0];
        if (size + 1 < rl.length - step) {
            return Arrays.copyOf(rl, rl.length - step);
        }
        else {
            return rl;
        }
    }
}
