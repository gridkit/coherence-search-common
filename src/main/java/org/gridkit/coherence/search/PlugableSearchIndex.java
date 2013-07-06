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

package org.gridkit.coherence.search;

import java.util.Map;
import java.util.Set;

import com.tangosol.util.filter.IndexAwareFilter;

/**
 * SPI interface to be implemented by provider of custom index engine.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 * @param <I> type of index instance
 * @param <IC> type of index configuration options, passed to index up on creation
 * @param <Q> type of query object for this index plugin
 */
public interface PlugableSearchIndex<I, IC, Q> {

	/**
	 * Due to usage of generic extractors and filters. A some token is required to distinguish
	 * different custom indexes.
	 * Token object should be compatible with cache serializer. 
	 */
	public Object createIndexCompatibilityToken(IC indexConfig);

	/**
	 * Creates instance of index structure using config.
	 * @param indexConfig
	 * @return index instance
	 */
    public I createIndexInstance(IC indexConfig);

    /**
     * Plugin has a last chance to enforce engine compabilities required
     * to index functioning. This method is being called just befor index creation and
     * may override settings done on {@link SearchFactory} level.
     * @param config
     */
    public void configure(IndexEngineConfig config);
        
    /**
     * Updates instance of index using change set map.
     */
    public void updateIndexEntries(I index, Map<Object, IndexUpdateEvent> events, IndexInvocationContext context);
    
    /**
     * See {@link IndexAwareFilter#calculateEffectiveness(Map, Set)}
     */
    public int calculateEffectiveness(I index, Q query, Set<Object> keySet, IndexInvocationContext context);
    
    /**
     * See {@link IndexAwareFilter#applyIndex(Map, Set)}
     * @return <code>true</code> if post filtering of results is required
     */
    public boolean applyIndex(I index, Q query, Set<Object> keySet, IndexInvocationContext context);
    
    /**
     * Evaluates if provided "document" (attribute being indexed) conforms query criteria.
     */
    public boolean evaluate(Q query, Object document);
    
}
