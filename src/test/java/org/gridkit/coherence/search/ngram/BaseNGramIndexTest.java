/**
 * Copyright 2011 Alexey Ragozin
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

package org.gridkit.coherence.search.ngram;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.coherence.search.SearchFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.extractor.IdentityExtractor;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public abstract class BaseNGramIndexTest {

	private int NGRAM_SIZE = 3;
    private static final String[] CHARSET1 = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O"};

	private NamedCache cache;
	private SearchFactory<NGramIndex, Integer, String> factory = new SearchFactory<NGramIndex, Integer, String>(new NGramIndexPlugin(), NGRAM_SIZE, IdentityExtractor.INSTANCE);
	
	@BeforeClass
	public static void configure() {
		CacheFactory.setConfigurableCacheFactory(new DefaultConfigurableCacheFactory("n-gram-cache-config.xml"));
	}
	
	@Before
	public void init() {
		cache = getCache();
		factory.createIndex(cache);		
	}
	
	public void init4096() {

		long objectCount = 4096;
		
        long rangeStart = 0;
        long rangeFinish = rangeStart + objectCount;
		
        int putSize = 10;
//        long blockTs = System.nanoTime();
//        long blockStart = rangeStart;
        for(long i = rangeStart;  i < rangeFinish; i += putSize) {
            if (i % 1000 == 0) {
//                String stats = "";
//                if (i > blockStart) {
//                    long blockSize = i - blockStart;
//                    long blockTime = System.nanoTime() - blockTs;
//                    double avg = (((double)blockSize) / blockTime) * TimeUnit.SECONDS.toNanos(1);
//                    stats = " block " + blockSize + " in " + TimeUnit.NANOSECONDS.toMillis(blockTime) + "ms, avg " + avg + " put/sec, batchSize " + putSize;
//                }
//                blockTs = System.nanoTime();
//                blockStart = i;
            }
            long j = Math.min(rangeFinish, i + putSize);
            cache.putAll(generateEncodedNumber(i, j, CHARSET1));
        }
	}
	
    static Map<String, String> generateEncodedNumber(long from, long to, String[] charset) {
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i != (to - from); ++i) {
            String text = encodeBitPositions(from + i, charset);
            result.put(text, text);
        }
        return result;
    }    
    
    static String encodeBitPositions(long value, String[] charset) {
        StringBuilder builder = new StringBuilder();
        for(int i =0 ; i != charset.length; ++i) {
            if ((value & (1 << i)) != 0) {
                builder.append(charset[i]);
            }
        }
        return builder.toString();
    }
	
	protected abstract NamedCache getCache();
	
	@After
	public void destroyCache() {
		cache.destroy();
	}
	
	@AfterClass
	public static void shutdown() {
		CacheFactory.getCluster().shutdown();
	}
	
	@Test
	public void testBulkQuery() {
		init4096();
		Assert.assertEquals(8, cache.keySet(factory.createFilter("CDEFGHIJK")).size());
		Assert.assertEquals(128, cache.keySet(factory.createFilter("ABCDE")).size());
		Assert.assertEquals(256, cache.keySet(factory.createFilter("BCDE")).size());
		Assert.assertEquals(256, cache.keySet(factory.createFilter("CDEF")).size());
		Assert.assertEquals(512, cache.keySet(factory.createFilter("DEF")).size());
		Assert.assertEquals(512, cache.keySet(factory.createFilter("EFG")).size());
		Assert.assertEquals(1024, cache.keySet(factory.createFilter("FG")).size());
		Assert.assertEquals(2048, cache.keySet(factory.createFilter("B")).size());
	}
}
