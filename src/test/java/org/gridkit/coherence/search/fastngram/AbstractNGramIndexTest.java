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
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Test {@link TrieNGramIndex} using {@link TextSearchIndex} contract.
 * 
 * @author Alexey Ragozin (alexey.ragozin@db.com)
 */
public abstract class AbstractNGramIndexTest {
	
	private static String[] TEXT_CORPUS = {
		"A quick brown fox",
		"MAKE IT QUICKLY",
		"foxhound",
		"-------------",
		"Few more foxes here",
		"",
	};
	
	public abstract TextSearchIndex newIndex();
	
	@Test
	public void verify_simple_search_cases() {
		TextSearchIndex index = newIndex();
		update(index, TEXT_CORPUS);
		assertSearch(index, "fox", 0, 2, 4);
		assertSearch(index, "quick", 0, 1);
		assertSearch(index, "---", 3);
		assertSearch(index, "more foxes", 4);
		assertSearch(index, "MORE FOXES", 4);
		assertSearch(index, "not found");
		assertSearch(index, "f", 0,2,4);
		assertSearch(index, " ", 0,1,4);
		assertSearch(index, "e ", 1,4);
	}

	@Test
	public void verify_simple_update_cases() {
		TextSearchIndex index = newIndex();
		update(index, TEXT_CORPUS);
		assertSearch(index, "fox", 0, 2, 4);
		assertSearch(index, "foxhound", 2);
		index.removeRow(2, TEXT_CORPUS[2]);
		assertSearch(index, "fox", 0, 4);
		index.addRow(2, "white fox");
		assertSearch(index, "fox", 0, 2, 4);
		assertSearch(index, "foxhound");
		assertSearch(index, "white", 2);
		assertSearch(index, "not found");
		assertSearch(index, "f", 0,2,4);
		assertSearch(index, " ", 0,1,2,4);
		assertSearch(index, "e ", 1,2,4);		
	}
	
	@Test
	public void random_correctness_test() {
		TextSearchIndex idx = newIndex();
		
		String[] tokens = new String[128];
		Random rnd = new Random(1);
		for(int i = 0; i != tokens.length; ++i) {
			int l = 1 + rnd.nextInt(6);
			char[] c = new char[l];
			for(int j = 0; j != c.length; ++j) {
				c[j] = (char)(' ' + rnd.nextInt(90));
			}
			tokens[i] = new String(c);
		}
		
		String[] table = new String[10000];
		int c = 0;
		for(int i = 0; i != 100000; ++i) {
			if (table.length * rnd.nextDouble() > c) {
				String text = tokenString(tokens, rnd, 6);
				int x = rnd.nextInt(table.length);
				if (table[x] != null) {
					idx.removeRow(x, table[x]);
				}
				else {
					++c;
				}
				table[x] = text;
				idx.addRow(x, text);
			}
			if (2 * table.length * rnd.nextDouble() < c) {
				int x = rnd.nextInt(table.length);
				if (table[x] != null) {
					idx.removeRow(x, table[x]);
					table[x] = null;
					--c;
				}
			}
			if (rnd.nextDouble() > 0.9) {
				if (rnd.nextDouble() > 0.2) {
					String token = tokens[rnd.nextInt(tokens.length)];
					while(token.length() < 4) {
						token = tokens[rnd.nextInt(tokens.length)];
					}
					verifyQuery(idx, table, token);
				}
				else {
					String text = tokenString(tokens, rnd, 3);
					verifyQuery(idx, table, randomSubstring(text, rnd, 4));
				}				
			}
		}		
	}

	private void verifyQuery(TextSearchIndex idx, String[] table, String substring) {
		int[] set = search(idx, substring);
		for(int i = 0; i != table.length; ++i) {
			if (table[i] != null) {
				if (idx.evaluate(table[i], substring)) {
					if (Arrays.binarySearch(set, i) < 0) {
						Assert.fail("Not in set: [" + i + "] [" + table[i] + "] - substring [" + substring + "]");
					}
				}
			}
		}		
	}

	private String tokenString(String[] tokens, Random rnd, int len) {
		StringBuilder sb = new StringBuilder();
		int l = 1 + rnd.nextInt(len);
		for(int n = 0; n != l; ++n) {
			sb.append(tokens[rnd.nextInt(tokens.length)]);
		}
		String text = sb.toString();
		return text;
	}

	private String randomSubstring(String base, Random rnd, int minlen) {
		if (base.length() > minlen) {
			base = base.substring(rnd.nextInt(base.length() - minlen));
		}
		if (base.length() > minlen) {
			base = base.substring(0, base.length() - rnd.nextInt(base.length() - minlen));
		}
		return base;
	}
	
	private void assertSearch(TextSearchIndex index, String query, int... expected) {
		int[] result = search(index, query);
		Assert.assertEquals(Arrays.toString(expected), Arrays.toString(result));
	}
	
	private void update(TextSearchIndex index, String[] corpus) {
		for(int i = 0; i != corpus.length; ++i) {
			index.addRow(i, corpus[i]);
		}
	}

	private int[] search(TextSearchIndex index, String query) {
		int[] buf = new int[16];
		int n = 0;
		IntSequence it = index.getCanditates(query);
		if (!it.isValid()) {
			it.advance();
		}
		while(it.isValid()) {
			if (n == buf.length) {
				buf = Arrays.copyOf(buf, buf.length * 2);
			}
			buf[n++] = it.getValue();
			it.advance();
		}
		return Arrays.copyOf(buf, n);
	}
	
//	private boolean verify(TextSearchIndex index, String[] corpus, String query, int row) {
//		return index.evaluate(corpus[row], query);
//	}	
}
