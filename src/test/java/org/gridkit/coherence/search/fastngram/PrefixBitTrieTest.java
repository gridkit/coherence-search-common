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

import org.junit.Assert;
import org.junit.Test;

public class PrefixBitTrieTest {
	
	PrefixBitTrie trie;
	long ops = 0;
	
	@Test
	public void small_test_62x0() {
		
		trie = new PrefixBitTrie(62, 0, 4);
		printTrie();
		trie.put(10);
		
		printTrie();
		
		Assert.assertEquals(1, trie.size());
		Assert.assertEquals(10, trie.get(10));
		Assert.assertEquals(~11l, trie.get(11));
		Assert.assertEquals(~128l, trie.get(128));

		trie.put(100);
		printTrie();
		
		Assert.assertEquals(2, trie.size());
		Assert.assertEquals(10, trie.get(10));
		Assert.assertEquals(~11l, trie.get(11));
		Assert.assertEquals(~128l, trie.get(128));
		Assert.assertEquals(100, trie.get(100));
		Assert.assertEquals(~101l, trie.get(101));
		
		trie.remove(10);
		printTrie();

		Assert.assertEquals(1, trie.size());
		Assert.assertEquals(~10l, trie.get(10));
		Assert.assertEquals(~11l, trie.get(11));
		Assert.assertEquals(~128l, trie.get(128));
		Assert.assertEquals(100, trie.get(100));
		Assert.assertEquals(~101l, trie.get(101));

		trie.put(0x1);
		trie.put(0x202);
		trie.put(0x3303);
		printTrie();
		
		Assert.assertEquals(4, trie.size());
		Assert.assertEquals(0x01, trie.get(0x01));
		Assert.assertEquals(0x202, trie.get(0x202));
		Assert.assertEquals(0x3303, trie.get(0x3303));
		Assert.assertEquals(100, trie.get(100));
		Assert.assertEquals(~101l, trie.get(101));
		
		trie.remove(0x202);
		printTrie();

		Assert.assertEquals(3, trie.size());
		Assert.assertEquals(0x01, trie.get(0x01));
		Assert.assertEquals(~0x202l, trie.get(0x202));
		Assert.assertEquals(0x3303, trie.get(0x3303));
		Assert.assertEquals(100, trie.get(100));
		Assert.assertEquals(~101l, trie.get(101));
		
		trie.put(0x3304);
		printTrie();
		
		trie.remove(0x3303);
		printTrie();

		Assert.assertEquals(3, trie.size());
		Assert.assertEquals(0x01, trie.get(0x01));
		Assert.assertEquals(~0x202l, trie.get(0x202));
		Assert.assertEquals(~0x3303l, trie.get(0x3303));
		Assert.assertEquals(0x3304, trie.get(0x3304));
		Assert.assertEquals(100, trie.get(100));
		Assert.assertEquals(~101l, trie.get(101));
		
		trie.remove(0x01);
		trie.remove(0x3304);
		trie.remove(100);
		printTrie();

		Assert.assertEquals(0, trie.size());
		Assert.assertEquals(1, trie.slotUsed());
		Assert.assertEquals(~0x01l, trie.get(0x01));
		Assert.assertEquals(~0x202l, trie.get(0x202));
		Assert.assertEquals(~0x3303l, trie.get(0x3303));
		Assert.assertEquals(~0x3304l, trie.get(0x3304));
		Assert.assertEquals(~100l, trie.get(100));
		Assert.assertEquals(~101l, trie.get(101));	
	}

	@Test
	public void small_test_64x0() {
		
		trie = new PrefixBitTrie(64, 0, 4);
		printTrie();
		trie.put(0x0000000000000000l);
		trie.put(0xffffffffffffffffl);
		
		printTrie();
		
		Assert.assertEquals(2, trie.size());
		Assert.assertEquals(0x0000000000000000l, trie.get(0x0000000000000000l));
		Assert.assertEquals(~0x7fffffffffffffffl, trie.get(0x7fffffffffffffffl));
		Assert.assertEquals(0xffffffffffffffffl, trie.get(0xffffffffffffffffl));
		
		trie.put(0x7fffffffffffffffl);
		printTrie();

		Assert.assertEquals(3, trie.size());
		Assert.assertEquals(0x0000000000000000l, trie.get(0x0000000000000000l));
		Assert.assertEquals(0x7fffffffffffffffl, trie.get(0x7fffffffffffffffl));
		Assert.assertEquals(0xffffffffffffffffl, trie.get(0xffffffffffffffffl));
	
		trie.remove(0xffffffffffffffffl);
		printTrie();

		Assert.assertEquals(2, trie.size());
		Assert.assertEquals(0x0000000000000000l, trie.get(0x0000000000000000l));
		Assert.assertEquals(0x7fffffffffffffffl, trie.get(0x7fffffffffffffffl));
		Assert.assertEquals(~0xffffffffffffffffl, trie.get(0xffffffffffffffffl));		
	}

	@Test
	public void small_test_63x1() {
		
		trie = new PrefixBitTrie(63, 1, 4);
		printTrie();
		trie.put(0x0000000000000000l);
		trie.put(0xffffffffffffffffl);
		
		printTrie();
		
		Assert.assertEquals(2, trie.size());
		Assert.assertEquals(0x0000000000000000l, trie.get(0x0000000000000000l));
		Assert.assertEquals(~0x7fffffffffffffffl, trie.get(0x7fffffffffffffffl));
		Assert.assertEquals(0xffffffffffffffffl, trie.get(0xffffffffffffffffl));
		
		trie.put(0x7fffffffffffffffl);
		printTrie();
		
		Assert.assertEquals(3, trie.size());
		Assert.assertEquals(0x0000000000000000l, trie.get(0x0000000000000000l));
		Assert.assertEquals(0x7fffffffffffffffl, trie.get(0x7fffffffffffffffl));
		Assert.assertEquals(0xffffffffffffffffl, trie.get(0xffffffffffffffffl));
		
		trie.remove(0xffffffffffffffffl);
		printTrie();
		
		Assert.assertEquals(2, trie.size());
		Assert.assertEquals(0x0000000000000000l, trie.get(0x0000000000000000l));
		Assert.assertEquals(0x7fffffffffffffffl, trie.get(0x7fffffffffffffffl));
		Assert.assertEquals(~0xffffffffffffffffl, trie.get(0xffffffffffffffffl));
		
		trie.put(0x0000000000000001l);

		Assert.assertEquals(2, trie.size());
		Assert.assertEquals(0x0000000000000001l, trie.get(0x0000000000000000l));
		Assert.assertEquals(0x7fffffffffffffffl, trie.get(0x7fffffffffffffffl));
		Assert.assertEquals(~0xffffffffffffffffl, trie.get(0xffffffffffffffffl));		
	}

	private void printTrie() {
		System.out.println(trie.dumpTree().printAsTree());
	}
	
	@Test
	public void small_test_16x4() {
		trie = new PrefixBitTrie(16, 4, 4);

		put16x4(10, 1);
		Assert.assertEquals(1, trie.size());
		Assert.assertEquals(1, get16x4(10));
		Assert.assertEquals(-1, get16x4(11));

		Assert.assertEquals(1, put16x4(10, 2));
		Assert.assertEquals(-1, put16x4(11, 3));
		Assert.assertEquals(2, trie.size());
		
		printTrie();
		
		put16x4(0x1234, 5);
		put16x4(0x2345, 6);
		put16x4(0x3456, 7);
		put16x4(0x4567, 8);
		put16x4(0x5679, 9);
		put16x4(0x679A, 10);
		put16x4(0x79AB, 11);

		printTrie();

		Assert.assertEquals(9, trie.size());
		Assert.assertEquals(2, get16x4(10));
		Assert.assertEquals(3, get16x4(11));
		Assert.assertEquals(7, get16x4(0x3456));
		Assert.assertEquals(11, get16x4(0x79AB));
		Assert.assertEquals(-1, get16x4(0x79AA));
		
		Assert.assertEquals(-1, put16x4(0x0000, 1));
		Assert.assertEquals(-1, put16x4(0x7000, 2));
		Assert.assertEquals(-1, put16x4(0xFFFF, 3));

		Assert.assertEquals(12, trie.size());
		Assert.assertEquals(1, get16x4(0x0000));
		Assert.assertEquals(2, get16x4(0x7000));
		Assert.assertEquals(3, get16x4(0xFFFF));

		Assert.assertEquals(6, remove16x4(0x2345));
		Assert.assertEquals(-1, remove16x4(0x2345));
		Assert.assertEquals(8, remove16x4(0x4567));
		Assert.assertEquals(-1, remove16x4(0x4567));
		Assert.assertEquals(-1, remove16x4(0x45AA));
		Assert.assertEquals(3, remove16x4(0xFFFF));
		Assert.assertEquals(9, trie.size());
		
		printTrie();
	}

	@Test
	public void verify_ceil_floor_16x4() {
	    trie = new PrefixBitTrie(16, 4, 4);
	    
	    Assert.assertEquals(0, trie.size());
	    Assert.assertEquals(-1, ceil16x4(10));
	    Assert.assertEquals(-1, floor16x4(10));

	    put16x4(1000, 1);

	    Assert.assertEquals(1, trie.size());
	    Assert.assertEquals(1000, ceil16x4(0));
	    Assert.assertEquals(1000, ceil16x4(10));
	    Assert.assertEquals(1000, ceil16x4(100));
	    Assert.assertEquals(1000, ceil16x4(999));
	    Assert.assertEquals(1000, ceil16x4(1000));
	    Assert.assertEquals(-1, ceil16x4(1001));
	    Assert.assertEquals(-1, ceil16x4(2000));
	    Assert.assertEquals(1000, floor16x4(2000));
	    Assert.assertEquals(1000, floor16x4(1900));
	    Assert.assertEquals(1000, floor16x4(1010));
	    Assert.assertEquals(1000, floor16x4(1001));
	    Assert.assertEquals(1000, floor16x4(1000));
	    Assert.assertEquals(-1, floor16x4(999));
	    Assert.assertEquals(-1, floor16x4(500));

	    put16x4(1001, 1);
	    
	    Assert.assertEquals(2, trie.size());
	    Assert.assertEquals(1000, ceil16x4(0));
	    Assert.assertEquals(1000, ceil16x4(10));
	    Assert.assertEquals(1000, ceil16x4(100));
	    Assert.assertEquals(1000, ceil16x4(999));
	    Assert.assertEquals(1000, ceil16x4(1000));
	    Assert.assertEquals(1001, ceil16x4(1001));
	    Assert.assertEquals(-1, ceil16x4(1002));
	    Assert.assertEquals(-1, ceil16x4(2000));
	    Assert.assertEquals(1001, floor16x4(2000));
	    Assert.assertEquals(1001, floor16x4(1900));
	    Assert.assertEquals(1001, floor16x4(1010));
	    Assert.assertEquals(1001, floor16x4(1002));
	    Assert.assertEquals(1001, floor16x4(1001));
	    Assert.assertEquals(1000, floor16x4(1000));
	    Assert.assertEquals(-1, floor16x4(999));
	    Assert.assertEquals(-1, floor16x4(500));

	    put16x4(2000, 1);

	    Assert.assertEquals(3, trie.size());
	    Assert.assertEquals(1000, ceil16x4(0));
	    Assert.assertEquals(1000, ceil16x4(10));
	    Assert.assertEquals(1000, ceil16x4(100));
	    Assert.assertEquals(1000, ceil16x4(999));
	    Assert.assertEquals(1000, ceil16x4(1000));
	    Assert.assertEquals(1001, ceil16x4(1001));
	    Assert.assertEquals(2000, ceil16x4(1002));
	    Assert.assertEquals(2000, ceil16x4(2000));
	    Assert.assertEquals(-1, ceil16x4(2001));
	    Assert.assertEquals(2000, floor16x4(3000));
	    Assert.assertEquals(2000, floor16x4(2000));
	    Assert.assertEquals(1001, floor16x4(1900));
	    Assert.assertEquals(1001, floor16x4(1010));
	    Assert.assertEquals(1001, floor16x4(1002));
	    Assert.assertEquals(1001, floor16x4(1001));
	    Assert.assertEquals(1000, floor16x4(1000));
	    Assert.assertEquals(-1, floor16x4(999));
	    Assert.assertEquals(-1, floor16x4(500));
	}

	@Test
	public void slot_relocation_test_16x4() {
		trie = new PrefixBitTrie(16, 4, 4);

		int size = 124;
		for(int i = 0; i != size; ++i) {
			put16x4(i << 8, i % 5);
		}
		
		printTrie();

		for(int i = 0; i != size; ++i) {
			Assert.assertEquals(i % 5, get16x4(i << 8));
		}	
	}
	
	@Test
	public void random_test_16x4() {
		trie = new PrefixBitTrie(16, 4, 4);

		RandomVerification verificator = new RandomVerification() {
			
			@Override
			int get(int key) {
				return get16x4(key);
			}

			@Override
            int ceil(int key) {
                return ceil16x4(key);
            }



            @Override
            int floor(int key) {
                return floor16x4(key);
            }

            @Override
			int put(int key, int val) {
				return put16x4(key, val);
			}

			@Override
			int remove(int key) {
				return remove16x4(key);
			}
		};
		
		verificator.verifications = 100;
		verificator.iterations = 10000;
		verificator.keyRange = 1 << 16;
		verificator.verify();
	}

	@Test
	public void random_test_60x4_compact() {
		trie = new PrefixBitTrie(60, 4, 4);

		RandomVerification verificator = new RandomVerification() {
			
			@Override
			int get(int key) {
				return get60x4(key);
			}

            @Override
            int ceil(int key) {
                return ceil60x4(key);
            }

            @Override
            int floor(int key) {
                return floor60x4(key);
            }

			@Override
			int put(int key, int val) {
				return put60x4(key, val);
			}

			@Override
			int remove(int key) {
				return remove60x4(key);
			}
		};
		
		verificator.verifications = 100;
		verificator.iterations = 10000;
		verificator.keyRange = 1 << 16;
		verificator.verify();
	}

	@Test
	public void random_test_60x4_scrambled() {
		trie = new PrefixBitTrie(60, 4, 4);

		RandomVerification verificator = new RandomVerification() {
			
		    {
		        // scrambling is not monotonous
		        checkCeilFloor = false;
		    }
		    
			@Override
			int get(int key) {
				return get60x4(scramble(key));
			}

			@Override
            int ceil(int key) {
                return ceil60x4(scramble(key));
            }

            @Override
            int floor(int key) {
                return floor60x4(scramble(key));
            }

            @Override
			int put(int key, int val) {
				return put60x4(scramble(key), val);
			}

			@Override
			int remove(int key) {
				return remove60x4(scramble(key));
			}

			private long scramble(int key) {
				long skey = key;
				skey = skey << 18 ^ key;
				skey = skey << 20 | key;
				return skey; 
			}
		};
		
		verificator.verifications = 100;
		verificator.iterations = 10000;
		verificator.keyRange = 1 << 16;
		verificator.verify();
	}
	
	protected int get16x4(int key) {
		++ops;
		long tkn = ((long)key) << 4;
		long r = trie.get(tkn);
		if (r < 0) {
			return -1;
		}
		else {
			return (int)(0xF & r);
		}
	}

	protected int ceil16x4(int key) {
	    ++ops;
	    long tkn = ((long)key) << 4;
	    long r = trie.getCeil(tkn);
	    if (r < 0) {
	        return -1;
	    }
	    else {
	        Assert.assertEquals(r, trie.get(r));
	        return (int)(r >> 4);
	    }
	}

	protected int floor16x4(int key) {
	    ++ops;
	    long tkn = ((long)key) << 4;
	    long r = trie.getFloor(tkn);
	    if (r < 0) {
	        return -1;
	    }
	    else {
	        Assert.assertEquals(r, trie.get(r));
	        return (int)(r >> 4);
	    }
	}

	protected int put16x4(int key, int val) {
		++ops;
		long tkn = ((long)key) << 4 | (0xF & val);
		if (ops == 1051170) {
			new String();
		}
		long r = trie.getAndPut(tkn);
		if (r < 0) {
			return -1;
		}
		else {
			return (int)(0xF & r);
		}
	}

	protected int remove16x4(int key) {
		++ops;
		long tkn = ((long)key) << 4;
		long r = trie.getAndRemove(tkn);
		if (r < 0) {
			return -1;
		}
		else {
			return (int)(0xF & r);
		}
	}

	protected int get60x4(long key) {
		++ops;
		long tkn = ((long)key) << 4;
		long r = trie.get(tkn);
		if (r == ~tkn) {
			return -1;
		}
		else {
			return (int)(0xF & r);
		}
	}
	
    protected int ceil60x4(long key) {
        ++ops;
        long tkn = ((long)key) << 4;
        long r = trie.getCeil(tkn);
        if (r == ~tkn) {
            return -1;
        }
        else {
            Assert.assertEquals(r, trie.get(r));
            return (int)(r >> 4);
        }
    }

    protected int floor60x4(long key) {
        ++ops;
        long tkn = ((long)key) << 4;
        long r = trie.getFloor(tkn);
        if (r == ~tkn) {
            return -1;
        }
        else {
            Assert.assertEquals(r, trie.get(r));
            return (int)(r >> 4);
        }
    }

    protected int put60x4(long key, int val) {
		++ops;
		long tkn = ((long)key) << 4 | (0xF & val);
		long r = trie.getAndPut(tkn);
		if (r == ~tkn) {
			return -1;
		}
		else {
			return (int)(0xF & r);
		}
	}
	
	protected int remove60x4(long key) {
		++ops;
		long tkn = ((long)key) << 4;
		long r = trie.getAndRemove(tkn);
		if (r == ~tkn) {
			return -1;
		}
		else {
			return (int)(0xF & r);
		}
	}
	
	protected abstract class RandomVerification {
		
		Random rnd = new Random(1);
		int keyRange = 1 << 16;
		int valueRange = 5;
		
		int verifications = 100;
		int iterations = 10000;
		
		boolean checkCeilFloor = true;
		
		public void verify() {
			byte[] map = new byte[keyRange];
			Arrays.fill(map, (byte)-1);
			int size = 0;
			for(int c = 0; c != verifications; ++c) {

				for(int i = 0; i != iterations; ++i) {
					int n;
					for(int j = 0; j != 3; ++j) {
						n = rnd.nextInt(map.length);
						Assert.assertEquals(map[n], remove(n));
						if (map[n] != -1) {
							--size;
						}
						map[n] = -1;
						Assert.assertEquals(size, trie.size());
					}
					for(int j = 0; j != 5; ++j) {
						n = rnd.nextInt(map.length);
						int v = rnd.nextInt(16);
						Assert.assertEquals(map[n], put(n, v));
						if (map[n] == -1) {
							++size;
						}
						map[n] = (byte) v;
						Assert.assertEquals(size, trie.size());
					}
					for(int j = 0; j != 10; ++j) {
						n = rnd.nextInt(map.length);
						Assert.assertEquals(map[n], get(n));
						if (checkCeilFloor && map[n] < 0) {
						    Assert.assertEquals(ceil(map, n), ceil(n));
						    Assert.assertEquals(floor(map, n), floor(n));
						}
					}
				}	
				
				System.out.println("Size: " + trie.size() + " Node: " + trie.slotUsed());
				
				for(int i = 0; i != map.length; ++i) {
					Assert.assertEquals(map[i], get(i));
				}
			}
		}

		protected int floor(byte[] map, int n) {
		    for(int i = n - 1; i >= 0; --i) {
		        if (map[i] >= 0) {
		            return i;
		        }
		    }
		    return -1;
        }

        protected int ceil(byte[] map, int n) {
            for(int i = n + 1; i < map.length; ++i) {
                if (map[i] >= 0) {
                    return i;
                }
            }
            return -1;
        }

        abstract int get(int key);

		abstract int ceil(int key);

		abstract int floor(int key);

		abstract int put(int key, int val);
		
		abstract int remove(int key);
	}
}
