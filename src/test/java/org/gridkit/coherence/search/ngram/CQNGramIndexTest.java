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

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.filter.AlwaysFilter;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CQNGramIndexTest extends BaseNGramIndexTest {

	@Override
	protected NamedCache getCache() {
		return new ContinuousQueryCache(CacheFactory.getCache("local-cache"), new AlwaysFilter());
	}
}
