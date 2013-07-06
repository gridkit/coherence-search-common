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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.gridkit.coherence.search.IndexUpdateEvent.Type;

import com.tangosol.io.Serializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IndexAwareExtractor;
import com.tangosol.util.filter.IndexAwareFilter;

/**
 * Central class in Coherence-Search API. It is used as a factory to
 * produce query filters and index extractors. 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 * @param <I> index instance type (see {@link PlugableSearchIndex})
 * @param <IC> index config type (see {@link PlugableSearchIndex})
 * @param <Q> query type (see {@link PlugableSearchIndex})
 */
@SuppressWarnings("rawtypes")
public class SearchFactory<I, IC, Q> {

	protected PlugableSearchIndex<I, IC, Q> indexPlugin;
	protected IC indexConfig;
	protected ValueExtractor extractor;
	protected IndexEngineConfig engineConfig = new DefaultIndexEngineConfig();
	
	protected Object token;
	
	/**
	 * @param plugin search index plugin
	 * @param config config for this instance of index
	 * @param extractor extractor to extract indexed attribute from object
	 */
	public SearchFactory(PlugableSearchIndex<I, IC, Q> plugin, IC config, ValueExtractor extractor) {
		this.indexPlugin = plugin;
		this.indexConfig = config;
		this.extractor = extractor;
		this.token = plugin.createIndexCompatibilityToken(indexConfig); 
	}

	/**
	 * @return mutable {@link IndexEngineConfig} which can be used to tune options
	 */
	public IndexEngineConfig getEngineConfig() {
		return engineConfig;
	}
	
	/**
	 * Creates index for provided {@link NamedCache}
	 * @param cache
	 */
	public void createIndex(NamedCache cache) {
		SearchIndexExtractor<I, IC, Q> extractor = createConfiguredExtractor();
		cache.addIndex(extractor, false, null);
	}
	
	protected SearchIndexExtractor<I, IC, Q> createConfiguredExtractor() {
		return new SearchIndexExtractor<I, IC, Q>(indexPlugin, token, indexConfig, engineConfig, extractor);
	}

	protected SearchIndexExtractor<I, IC, Q> createFilterExtractor() {
		return new SearchIndexExtractor<I, IC, Q>(indexPlugin, token, extractor);
	}

	/**
	 * Create query based filter. An index should be created using {@link #createIndex(NamedCache)} before using of such filter.
	 * @param query search query, specific to plugin
	 * @return Coherence filter
	 */
	public Filter createFilter(Q query) {
		return new QueryFilter<I, Q>(createFilterExtractor(), query);
	}
	
	public static interface SearchIndexWrapper<I> extends MapIndex, IndexInvocationContext {
		
		public <R> R callCoreIndex(SearchIndexCallable<I, R> callable);		
	}
	
	public static interface SearchIndexCallable<I, R> {
		
		public R execute(I index);		
	}
	
	static class SearchIndexEngine<I,Q> implements MapIndex, IndexInvocationContext, SearchIndexWrapper<I> {

		private static Timer INDEX_TIMER = new Timer("IndexFlushTimer", true);
		
		private PlugableSearchIndex<I, ?, Q> psi;
		
		private ValueExtractor attributeExtrator;
		private I coreIndex;
		private MapIndex attributeIndex;
		private TimerTask flushTask;
		
		private Map<Object, IndexUpdateEvent> pendingUpdates;
		
		private int queueSizeLimit = 0;
		private int indexingDelay = 0;
		private boolean originalValueForUpdates = true;
		
		private boolean binaryMode = false;
		private Serializer serializer = null;
		
		public SearchIndexEngine(I index, ValueExtractor extractor, PlugableSearchIndex<I, ?, Q> psi) {
			this.coreIndex = index;
			this.attributeExtrator = extractor;
			this.psi = psi;
		}

		public void init(IndexEngineConfig config, Map<Object, Object> indexMap) {
			configure(config);
			if (attributeIndex != null) {
				if (indexMap.get(attributeExtrator) == null) {
					indexMap.put(attributeExtrator, indexMap);
				}
			}
		}
		
		private void configure(IndexEngineConfig config) {

			psi.configure(config);			
			if (config.isAttributeIndexEnabled()) {
				attributeIndex = new SimpleMapIndex(attributeExtrator, false, null, null);
			}
		
			queueSizeLimit = config.getIndexUpdateQueueSizeLimit();
			indexingDelay = config.getIndexUpdateDelay();
			originalValueForUpdates = config.isOldValueOnUpdateEnabled();
			
			if (queueSizeLimit > 0) {
				pendingUpdates = new HashMap<Object, IndexUpdateEvent>();
				if (indexingDelay > 0) {
					scheduleFlasher(indexingDelay);
				}
			}
		}
		
		private void scheduleFlasher(int indexingDelay) {
			flushTask = new TimerTask() {
				@Override
				public void run() {
					flush();
				}
			};
			INDEX_TIMER.schedule(flushTask , indexingDelay, indexingDelay);
		}

		public void tearDown(Map<?, ?> indexMap) {
			if (attributeIndex == indexMap.get(attributeExtrator)) {
				indexMap.remove(attributeExtrator);
			}
			if (flushTask != null) {
				flushTask.cancel();
			}
		}
		
		@Override
		public void insert(Entry entry) {
			checkMode(entry);
			Object key = getKeyFromEntry(entry);
			Object value = getValueFromEntry(entry);
			IndexUpdateEvent event = new IndexUpdateEvent(key, value, null, Type.INSERT);
			if (attributeIndex != null) {
				attributeIndex.delete(entry);
			}
			enqueue(event);
		}

		@Override
		public void update(Entry entry) {
			checkMode(entry);
			Object key = getKeyFromEntry(entry);
			Object value = getValueFromEntry(entry);
			Object oldValue = getOriginalValueFromEntry(entry);
			IndexUpdateEvent event = new IndexUpdateEvent(key, value, oldValue, Type.UPDATE);
			if (attributeIndex != null) {
				attributeIndex.delete(entry);
			}
			enqueue(event);
		}
		
		@Override
		public void delete(Entry entry) {
			checkMode(entry);
			Object key = getKeyFromEntry(entry);
			Object oldValue = getOriginalValueFromEntry(entry);
			IndexUpdateEvent event = new IndexUpdateEvent(key, null, oldValue, Type.DELETE);
			if (attributeIndex != null) {
				attributeIndex.delete(entry);
			}
			enqueue(event);
		}

		private void checkMode(Entry entry) {
			// TODO optimize
			if (!binaryMode) {
				if (entry instanceof BinaryEntry) {
					binaryMode = true;
					serializer = ((BinaryEntry)entry).getSerializer();
				}
			}			
		}

		private Object getKeyFromEntry(Entry entry) {
			if (entry instanceof BinaryEntry) {
				return ((BinaryEntry)entry).getBinaryKey();
			}
			else {
				return entry.getKey(); 
			}
		}

		private Object getValueFromEntry(Entry entry) {
			// TODO extractor helper
			return attributeExtrator.extract(entry.getValue());
		}

		private Object getOriginalValueFromEntry(Entry entry) {
			if (!originalValueForUpdates) {
				return null;
			}
			else {
				if (entry instanceof BinaryEntry) {
					return attributeExtrator.extract(((BinaryEntry)entry).getOriginalValue());
				}
				else {
					if (attributeIndex != null) {
						return attributeIndex.get(getKeyFromEntry(entry));
					}
					else {
						throw new RuntimeException("Forward attribute index required");
					}
				}
			}
		}

		private void enqueue(IndexUpdateEvent event) {
			if (queueSizeLimit == 0) {
				psi.updateIndexEntries(coreIndex, Collections.singletonMap(event.getKey(), event), this);
			}
			else {
				synchronized (this) {
					IndexUpdateEvent old = pendingUpdates.get(event.getKey());
					if (old != null) {
						old.merge(event);
					}
					else {
						pendingUpdates.put(event.getKey(), event);
					}
				}
				if (pendingUpdates.size() >= queueSizeLimit) {
					flush();
				}
			}
		}
		
		private synchronized void flush() {
			if (pendingUpdates != null && pendingUpdates.size() > 0) {
				psi.updateIndexEntries(coreIndex, pendingUpdates, this);
				pendingUpdates.clear();
			}
		}

		@Override
		public Object get(Object key) {
			return NO_VALUE;
		}

		@Override
		public Comparator getComparator() {
			return null;
		}

		@Override
		public Map getIndexContents() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ValueExtractor getValueExtractor() {
			return attributeExtrator;
		}

		@Override
		public boolean isOrdered() {
			return false;
		}

		@Override
		public boolean isPartial() {
			return false;
		}

		@Override
		public Binary ensureBinaryKey(Object key) {
			if (key instanceof Binary) {
				return (Binary) key;
			}
			else {
				// TODO proper serializer support
				if (serializer != null) {
					return ExternalizableHelper.toBinary(key, serializer);
				}
				else {
					return ExternalizableHelper.toBinary(key);
				}
			}
		}

		@Override
		public Object ensureObjectKey(Object key) {
			if (key instanceof Binary) {
				if (serializer != null) {
					return ExternalizableHelper.fromBinary((Binary) key, serializer);
				}
				else {
					return ExternalizableHelper.fromBinary((Binary) key);
				}
			}
			else {
				// TODO proper serializer support
				return key;
			}
		}
		

		@Override
		public Object ensureFilterCompatibleKey(Object key) {
			if (binaryMode) {
				return ensureBinaryKey(key);
			}
			else {
				return ensureObjectKey(key);
			}
		}

		@Override
		public Object getRawAttribute(Object key) {
			if (attributeIndex != null) {
				// TODO ensure compatible key format
				return attributeIndex.get(key);
			}
			throw new UnsupportedOperationException("Raw attribute index is not configured");
		}
		
		
		
		public Filter applyIndex(QueryFilter<I, Q> filter, Set<Object> keys) {
			flush();
			boolean dirty = psi.applyIndex(coreIndex, filter.getQuery(), keys, this);
			if (dirty) {
				return filter;
			}
			else {
				return null;
			}
		}

		public int calculateEffectiveness(QueryFilter<I, Q> filter, Set<Object> keys) {
			flush();
			return psi.calculateEffectiveness(coreIndex, filter.getQuery(), keys, this);
		}

		@Override
		public synchronized <R> R callCoreIndex(SearchIndexCallable<I, R> callable) {
			flush();
			return callable.execute(coreIndex);
		}
	}
	
	public static class SearchIndexExtractor<I, IC, Q> implements IndexAwareExtractor, Serializable, PortableObject {

		private static final long serialVersionUID = 20100813L;
		
		private PlugableSearchIndex<I, IC, Q> psi;
		private Object token;
		private IC indexConfiguration;
		private IndexEngineConfig engineConfig;
		private ValueExtractor extractor;
		
		public SearchIndexExtractor() {
			// for remoting
		}
		
		public SearchIndexExtractor(PlugableSearchIndex<I, IC, Q> psi, Object token, IC indexConfiguration,	IndexEngineConfig engineConfig, ValueExtractor extractor) {
			this.psi = psi;
			this.token = token;
			this.indexConfiguration = indexConfiguration;
			this.engineConfig = engineConfig;
			this.extractor = extractor;
		}

		public SearchIndexExtractor(PlugableSearchIndex<I, IC, Q> psi, Object token, ValueExtractor extractor) {
			this.psi = psi;
			this.token = token;
			this.extractor = extractor;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((token == null) ? 0 : token.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SearchIndexExtractor other = (SearchIndexExtractor) obj;
			if (token == null) {
				if (other.token != null)
					return false;
			} else if (!token.equals(other.token))
				return false;
			return true;
		}

		//Override in Coherence 3.6
		@SuppressWarnings("unchecked")
		public MapIndex createIndex(boolean sorted, Comparator comparator, Map indexMap) {
			SearchIndexEngine<I, Q> engine = new SearchIndexEngine<I, Q>(psi.createIndexInstance(indexConfiguration), extractor, psi);
			engine.init(engineConfig, indexMap);
			indexMap.put(this, engine);
			return engine;
		}
		
		

		//@Override in Coherence 3.7
		public MapIndex createIndex(boolean sorted, Comparator comparator, Map indexMap, BackingMapContext backingMapContext) {
			// TODO use backing map information to full extent
			return createIndex(sorted, comparator, indexMap);
		}

		@Override
		@SuppressWarnings("unchecked")
		public MapIndex destroyIndex(Map indexMap) {
			SearchIndexEngine<I, Q> engine = (SearchIndexEngine<I, Q>) indexMap.get(this);
			if (engine != null) {
				indexMap.remove(this);
				engine.tearDown(indexMap);
			}
			return engine;
		}

		@Override
		public Object extract(Object object) {
			return extractor.extract(object);
		}

		public PlugableSearchIndex<I, IC, Q> getPSI() {
			return psi;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void readExternal(PofReader in) throws IOException {
			int i = 1;
			psi = (PlugableSearchIndex<I, IC, Q>) in.readObject(i++);
			token = in.readObject(i++);
			indexConfiguration = (IC) in.readObject(i++);
			engineConfig = (IndexEngineConfig) in.readObject(i++);
			extractor = (ValueExtractor) in.readObject(i++);
		}

		@Override
		public void writeExternal(PofWriter out) throws IOException {
			int i = 1;
			out.writeObject(i++, psi);
			out.writeObject(i++, token);
			out.writeObject(i++, indexConfiguration);
			out.writeObject(i++, engineConfig);
			out.writeObject(i++, extractor);
		}
	}
	
	public static class QueryFilter<I, Q> implements IndexAwareFilter, Serializable {
		
		private static final long serialVersionUID = 20100813L;

		protected SearchIndexExtractor<I, ?, Q> extractor;
		// Lucene 4.0 requires some trickery
		protected Object query;
		
		public QueryFilter() {
			// serialization
		}
		
		public QueryFilter(SearchIndexExtractor<I, ?, Q> extractor, Object query) {
			this.extractor = extractor;
			this.query = query;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((extractor == null) ? 0 : extractor.hashCode());
			result = prime * result + ((query == null) ? 0 : query.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QueryFilter other = (QueryFilter) obj;
			if (extractor == null) {
				if (other.extractor != null)
					return false;
			} else if (!extractor.equals(other.extractor))
				return false;
			if (query == null) {
				if (other.query != null)
					return false;
			} else if (!query.equals(other.query))
				return false;
			return true;
		}

		@SuppressWarnings("unchecked")
		public Q getQuery() {
			return (Q)getRawQuery();
		}

		protected Object getRawQuery() {
			return query;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public Filter applyIndex(Map indexes, Set keys) {
			MapIndex index = (MapIndex) indexes.get(extractor);
			if (index != null) {
				SearchIndexEngine engine = (SearchIndexEngine) index;
				return engine.applyIndex(this, keys);
			}
			else {
				return this;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public int calculateEffectiveness(Map indexes, Set keys) {
			MapIndex index = (MapIndex) indexes.get(extractor);
			if (index != null) {
				SearchIndexEngine engine = (SearchIndexEngine) index;
				return engine.calculateEffectiveness(this, keys);
			}
			else {
				// 100x just for no reason
				return 100 * keys.size();
			}
		}

		@Override
		public boolean evaluateEntry(Entry entry) {
			return evaluate(entry.getValue());
		}

		@Override
		public boolean evaluate(Object object) {
			return extractor.getPSI().evaluate(getQuery(), extractor.extract(object));
		}
	}
}
