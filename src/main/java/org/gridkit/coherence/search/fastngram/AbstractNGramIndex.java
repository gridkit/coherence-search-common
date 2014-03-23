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
 * This class implements n-gram based search algorithm, 
 * but keeps itself abstracted from physical structure of index. 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public abstract class AbstractNGramIndex implements TextSearchIndex {

	// Could be 2, 3, 4 - should be packed to int
    protected final int ngramSize;
    protected final int bitsPerChar;
    protected final int charMask;
    
    
    /**
     * @param ngramSize
     * @param bitsPerChar number of bits to truncate characters in indexed text
     */
    public AbstractNGramIndex(int ngramSize, int bitsPerChar) {
        this.ngramSize = ngramSize;
        if (ngramSize < 2 || ngramSize > 4) {
        	throw new IllegalArgumentException();
        }
        this.bitsPerChar = bitsPerChar;
        if (bitsPerChar > 7 || bitsPerChar < 5) {
            throw new IllegalArgumentException("bitsPerChar should be in range [7, 8]");
        }
        charMask = -1 >>> (32 - bitsPerChar);        
    }

    @Override
	public boolean evaluate(String text, String query) {
    	if (query == null || query.length() == 0) {
    		return true;
    	}
    	if (text == null || text.length() == 0) {
    		return false;
    	}
    	// Would some fancy substring matching algorithm would make sense here?
		return text.toLowerCase().contains(query.toLowerCase());
	}

	@Override
	public boolean isIndexOnly(String query) {
		return query == null || query.length() < ngramSize;
	}

	@Override
	public boolean screen(int row, String query) {
		if (query == null || query.length() == 0) {
			throw new IllegalArgumentException("Query is empty");
		}
		if (query.length() < ngramSize) {
			int qterm = subterm(query);
			int qlen = query.length();
			int nextterm = 0;
			while(true) {
				int t = seekTerm(nextterm);
				if (t == -1) {
					return false;
				}
				if (contains(t, qterm, qlen)) {
					return true;
				}
				nextterm = t + 1;
			}
		}
		else {
			int tc = queryTermCount(query);
			for(int i = 0; i != tc; ++i) {
				int term = queryTerm(query, i);
				if (!imapContains(term, row)) {
					return false;
				}
			}
			return true;
		}
	}
	
	@Override
	public IntSequence getCanditates(String query) {
		if (query == null || query.length() == 0) {
			throw new IllegalArgumentException("Query is empty");
		}
		if (query.length() < ngramSize) {
			int qterm = subterm(query);
			int qlen = query.length();
			int nextterm = 0;
			int n = 0;
			int[] terms = new int[32];
			while(true) {
				int t = seekTerm(nextterm);
				if (t == -1) {
					break;
				}
				if (contains(t, qterm, qlen)) {
					if (n == terms.length) {
						terms = Arrays.copyOf(terms, 2 * terms.length);
					}
					terms[n++] = t;
				}
				nextterm = t + 1;
			}
			if (n == 0) {
				return new EmptySequence();
			}
			else {
				return new UnionSequence(Arrays.copyOf(terms, n));
			}
		}
		else {
			int tc = queryTermCount(query);
			int[] terms = new int[tc];
			for(int i = 0; i != tc; ++i) {
				terms[i] = queryTerm(query, i);
			}
			return new IntersectSequence(terms);
		}
	}

	@Override
	public void addRow(int rowId, String text) {
		verifyRowRange(rowId);
		int tc = textTermCount(text);
		for(int i = 0; i != tc; ++i) {
			int term = textTerm(text, i);
			imapAdd(term, rowId);
		}
	}

	@Override
	public void removeRow(int rowId, String text) {
		verifyRowRange(rowId);
		int tc = textTermCount(text);
		for(int i = 0; i != tc; ++i) {
			int term = textTerm(text, i);
			imapRemove(term, rowId);
		}
	}

	protected int textTermCount(String text) {
		// dense tokenizing strategy should be used for text
		return denseCoverageTermCount(text);
	}
	
	protected int textTerm(String text, int i) {
		// dense tokenizing strategy should be used for text
		return denseCoverageTerm(text, i);
	}

	protected int queryTermCount(String query) {
		// in theory query tokenizer could account term frequency
		// but so far simple strategy in implemented
		return minCoverageTermCount(query);
	}
	
	protected int queryTerm(String query, int i) {
		// in theory query tokenizer could account term frequency
		// but so far simple strategy in implemented
		return minConverageTerm(query, i);
	}
	
	protected int minCoverageTermCount(String text) {
		return (text.length() + ngramSize - 1) / ngramSize;
	}

	protected int minConverageTerm(String text, int i) {
		int f = i * ngramSize;
		int l = f + ngramSize;
		if (l > text.length()) {
			f -= l - text.length();
			if (f < 0) {
				f = 0;
			}
		}
		return term(text, f);
	}
	
	protected int denseCoverageTermCount(String text) {
		return text.length() < ngramSize ? 1 : text.length() - ngramSize + 1;
	}

	protected int denseCoverageTerm(String text, int i) {
		return term(text, i);
	}

    protected int term(String text, int f) {
        int tkn = 0;
        for(int i = 0; i != ngramSize; ++i) {
            int c = text.length() > f + i ? normalizeChar(text.charAt(f + i)) : 0;
            tkn = (tkn << bitsPerChar) | (charMask & c); 
        }
        return tkn;
    }

    protected String termToText(int term) {
        char[] c= new char[ngramSize];
        for(int i = ngramSize - 1; i >= 0; --i) {
            c[i] = (char)(charMask & term);
            term >>= bitsPerChar;
        }
        return new String(c);
    }

    protected int subterm(String query) {
        int tkn = 0;
        for(int i = 0; i != query.length(); ++i) {
            int c = normalizeChar(query.charAt(i));
            tkn = (tkn << bitsPerChar) | (charMask & c); 
        }
        return tkn;
    }

    protected boolean contains(int term, int subterm, int charCount) {
        int mask = (int)(-1 >>> (32 - (bitsPerChar * charCount)));
        for(int i = 0; i <= ngramSize - charCount; ++i) {
            if ((term & mask) == subterm) {
                return true;
            }           
            mask = mask << bitsPerChar;
            subterm = subterm << bitsPerChar;
        }
        return false;
    }
	
	protected char normalizeChar(char ch) {
		int c = 0x7F & Character.toLowerCase(ch);
		return (char)c;
	}
	
    protected abstract int seekTerm(int term);
    
	protected abstract int seekTermRow(int term, int rowId);
	
	private void verifyRowRange(int rowId) {
		if (rowId < 0) {
			throw new IllegalArgumentException("rowId is out of bounds [0, Integer.MAX_VALUE]");
		}		
	}

	protected abstract boolean imapContains(int term, int row);
	
	protected abstract void imapAdd(int term, int row);

	protected abstract void imapRemove(int term, int row);
	
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