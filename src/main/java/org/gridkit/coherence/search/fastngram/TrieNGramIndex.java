/**
 * Copyright 2014 Alexey Ragozin
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


/**
 * Implementation of n-gram index using trie to store index
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class TrieNGramIndex extends AbstractNGramIndex {

    private static final long MASK32 = 0xFFFFFFFFl;
    
    protected final long termMask;
    protected final PrefixBitTrie invertedMap;

    public TrieNGramIndex(int ngramSize, int bitsPerChar, int initilaTriePages) {
        super(ngramSize, bitsPerChar);
        int termBits = bitsPerChar * ngramSize;
        if (termBits > 30) {
            throw new IllegalArgumentException("Term length " + termBits + " bits is above 30 bit limit");
        }
        termMask = -1l >>> (64 - termBits);
        invertedMap = new PrefixBitTrie(termBits + 32, 0, initilaTriePages);
    }

    protected int seekTerm(int term) {
        long itkn = itnk(term, 0);
        long r = invertedMap.getCeil(itkn);
        if (r == ~itkn) {
            return -1;
        }
        else {
            return itknToTerm(r);
        }
    }
    
	protected int seekTermRow(int term, int rowId) {
	    long itkn = itnk(term, rowId);
	    long r = invertedMap.getCeil(itkn);
	    int rt = itknToTerm(r);
	    if (rt == term) {
	        return itknToRowId(r);
	    }
	    else {
	        return -1;
	    }
	}

	protected boolean imapContains(int term, int row) {
	    long itkn = itnk(term, row);
	    long r = invertedMap.get(itkn);
	    return r == itkn;
	}

	protected void imapAdd(int term, int row) {
	    invertedMap.put(itnk(term, row));
	}

	protected void imapRemove(int term, int row) {
	    invertedMap.remove(itnk(term, row));
	}
	
	protected long itnk(int term, int rowId) {
		return (((long)term) << 32) | (((long)rowId) & MASK32);
	}

	protected int itknToRowId(long itnk) {
		return (int)itnk;
	}

	protected int itknToTerm(long itnk) {
		return (int)(itnk >> 32);
	}

	protected static class EmptySequence implements IntSequence {

		@Override
		public boolean isValid() {
			return false;
		}

		@Override
		public boolean advance() {
			return false;
		}

		@Override
		public int getValue() {
			return -1;
		}
	}
	
	protected class IntersectSequence implements IntSequence {
		
		int[] terms;
		int[] cursors;
		int row = -1;
		
		public IntersectSequence(int[] terms) {
			this.terms = terms;
			this.cursors = new int[terms.length];
			for(int i = 0; i != cursors.length; ++i) {
				this.cursors[i] = seekTermRow(terms[i], 0);
			}
			seek();
		}

		private void seek() {
			int quorum = 0;
			int n = 0;
			int b = row + 1;
			while(quorum < terms.length) {
				int c = cursors[n];
				if (c == b) {
					++quorum;
				}
				else if (c > b) {
					b = c;
					quorum = 0;
				}
				else {
					// c < b
					cursors[n] = seekTermRow(terms[n], b);
					if (cursors[n] < 0) {
						// no more rows
						row = Integer.MIN_VALUE;
						return;
					}
					else {
						continue;
					}
				}
				n = (n + 1) % terms.length;
			}		
			row = b;
		}

		@Override
		public boolean isValid() {
			return row >= 0;
		}

		@Override
		public boolean advance() {
			if (row >= 0) { 
				seek();
			}
			return row >= 0;
		}

		@Override
		public int getValue() {
			return row;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(row).append(" [");
			for(int i = 0; i != terms.length; ++i) {
				sb.append("'").append(termToText(terms[i])).append("'");
				sb.append(":").append(cursors[i]);
				sb.append(',');
			}
			sb.setLength(sb.length() - 1);
			sb.append("]");
			return sb.toString();
		}
	}

	protected class UnionSequence implements IntSequence {

		int[] terms;
		int[] cursors;
		int row = Integer.MAX_VALUE;
		
		public UnionSequence(int[] terms) {
			this.terms = terms;
			this.cursors = new int[terms.length];
			for(int i = 0; i != cursors.length; ++i) {
				this.cursors[i] = seekTermRow(terms[i], 0);
			}
			seek();
		}

		private void seek() {
			int min = Integer.MAX_VALUE;
			for(int i = 0; i != cursors.length; ++i) {
				if (cursors[i] >= 0 && cursors[i] < min) {
					min = cursors[i];
				}
			}
			row = min;
			if (min != Integer.MAX_VALUE) {
				// advance cursors
				for(int i = 0; i != cursors.length; ++i) {
					if (cursors[i] == min) {
						cursors[i] = seekTermRow(terms[i], cursors[i] + 1);
					}
				}
			}
		}

		@Override
		public boolean isValid() {
			return row != Integer.MAX_VALUE;
		}

		@Override
		public boolean advance() {
			if (row != Integer.MAX_VALUE) { 
				seek();
			}
			return isValid();
		}

		@Override
		public int getValue() {
			return row;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(row == Integer.MAX_VALUE ? "eos" : String.valueOf(row)).append(" [");
			for(int i = 0; i != terms.length; ++i) {
				sb.append("'").append(termToText(terms[i])).append("'");
				sb.append(":").append(cursors[i]);
				sb.append(',');
			}
			sb.setLength(sb.length() - 1);
			sb.append("]");
			return sb.toString();
		}		
	}	
}