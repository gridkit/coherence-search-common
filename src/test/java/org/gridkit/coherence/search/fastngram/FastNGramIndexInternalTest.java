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

import org.junit.Assert;
import org.junit.Test;

/**
 * This class verifies semantic of internal methods of {@link FastNGramIndex}
 * which are not bound by external contracts.
 * 
 * @author Alexey Ragozin (alexey.ragozin@db.com)
 */
public class FastNGramIndexInternalTest {

	@Test
	public void verify_valid_ngram_size() {
		new FastNGramIndex(2, 7);
		new FastNGramIndex(3, 5);
		new FastNGramIndex(4, 6);
		try {
			new FastNGramIndex(1, 128);
			Assert.fail();
		}
		catch(IllegalArgumentException e) {			
		}
		try {
			new FastNGramIndex(5, 128);
			Assert.fail();
		}
		catch(IllegalArgumentException e) {			
		}
		try {
		    new FastNGramIndex(3, 4);
		    Assert.fail();
		}
		catch(IllegalArgumentException e) {			
		}
		try {
		    new FastNGramIndex(3, 8);
		    Assert.fail();
		}
		catch(IllegalArgumentException e) {			
		}
		try {
		    new FastNGramIndex(4, 7);
		    Assert.fail();
		}
		catch(IllegalArgumentException e) {			
		}
	}

	@Test
	public void verify_inverted_map() {
	    FastNGramIndex index = new FastNGramIndex(3, 6);
	    index.imapAdd(100, 10);
	    index.imapAdd(200, 20);
	    index.imapAdd(300, 30);
	    index.imapAdd(200, 120);
	    
	    Assert.assertTrue(index.imapContains(100, 10));
	    Assert.assertFalse(index.imapContains(100, 20));
	    Assert.assertTrue(index.imapContains(200, 20));
	    Assert.assertFalse(index.imapContains(200, 30));
	    Assert.assertTrue(index.imapContains(200, 120));
	    Assert.assertTrue(index.imapContains(300, 30));
	    
	    Assert.assertEquals(10, index.seekTermRow(100, 0));
	    Assert.assertEquals(10, index.seekTermRow(100, 10));
	    Assert.assertEquals(-1, index.seekTermRow(100, 11));
	    Assert.assertEquals(20, index.seekTermRow(200, 0));
	    Assert.assertEquals(20, index.seekTermRow(200, 20));
	    Assert.assertEquals(120, index.seekTermRow(200, 21));
	    Assert.assertEquals(-1, index.seekTermRow(200, 121));
	    
	    Assert.assertEquals(100, index.seekTerm(0));
	    Assert.assertEquals(100, index.seekTerm(100));
	    Assert.assertEquals(200, index.seekTerm(101));
	    Assert.assertEquals(200, index.seekTerm(200));
	    Assert.assertEquals(300, index.seekTerm(201));
	    
	    index.imapRemove(100, 10);

	    Assert.assertEquals(200, index.seekTerm(0));

	    index.imapRemove(200, 20);
	    Assert.assertEquals(200, index.seekTerm(0));
	    Assert.assertEquals(120, index.seekTermRow(200, 0));
	    
	    index.imapAdd(200, 50);
	    index.imapAdd(200, 30);
	    index.imapAdd(200, 40);
	    index.imapAdd(200, 10);
	    index.imapAdd(200, 11);
	    index.imapAdd(200, 12);
	    index.imapAdd(200, 13);
	    index.imapAdd(200, 14);
	    index.imapAdd(200, 15);

	    Assert.assertEquals(10, index.seekTermRow(200, 0));
	    Assert.assertEquals(10, index.seekTermRow(200, 10));
	    Assert.assertEquals(11, index.seekTermRow(200, 11));
	    Assert.assertEquals(12, index.seekTermRow(200, 12));
	    Assert.assertEquals(13, index.seekTermRow(200, 13));
	    Assert.assertEquals(14, index.seekTermRow(200, 14));
	    Assert.assertEquals(15, index.seekTermRow(200, 15));
	    Assert.assertEquals(30, index.seekTermRow(200, 16));
	    Assert.assertEquals(40, index.seekTermRow(200, 31));
	    Assert.assertEquals(50, index.seekTermRow(200, 41));
	    Assert.assertEquals(120, index.seekTermRow(200, 51));

	    index.imapRemove(200, 12);
	    index.imapRemove(200, 15);
	    index.imapRemove(200, 40);
	    index.imapRemove(200, 13);
	    index.imapRemove(200, 30);

        Assert.assertEquals(10, index.seekTermRow(200, 0));
        Assert.assertEquals(10, index.seekTermRow(200, 10));
        Assert.assertEquals(11, index.seekTermRow(200, 11));
        Assert.assertEquals(14, index.seekTermRow(200, 12));
        Assert.assertEquals(50, index.seekTermRow(200, 15));
        Assert.assertEquals(120, index.seekTermRow(200, 51));
	}
	
}
