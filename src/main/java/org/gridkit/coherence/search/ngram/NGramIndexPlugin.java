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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gridkit.coherence.search.IndexEngineConfig;
import org.gridkit.coherence.search.IndexInvocationContext;
import org.gridkit.coherence.search.IndexUpdateEvent;
import org.gridkit.coherence.search.PlugableSearchIndex;

/**
 * Reference implementation of custom index.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NGramIndexPlugin implements PlugableSearchIndex<NGramIndex, Integer, String> {

	@Override
	public boolean applyIndex(NGramIndex index, String query, Set<Object> keySet, IndexInvocationContext context) {
        return ngramFilter(index, query, keySet);
    }

    @SuppressWarnings("rawtypes")
	private boolean ngramFilter(NGramIndex index, String substring, Set setKeys) {
        if (index.getNGramSize() <= substring.length()) {
            filterSet(index, substring, setKeys);
            return true;
        }
        else {
            unionFilterSet(index, substring, setKeys);
            return false;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void unionFilterSet(NGramIndex index, String substring, Set setKeys) {
        Set result = new HashSet();
        for(Object entry: index.getNGramMap().entrySet()) {
            String ngram = (String) ((Map.Entry)entry).getKey();
            NGramRefList list = (NGramRefList) ((Map.Entry)entry).getValue();
            
            if (ngram.indexOf(substring) >= 0) {
                for(Object key: list.references.keySet()) {
                    if (setKeys.contains(key)) {
                        result.add(key);
                    }
                }
            }            
        }
        setKeys.retainAll(result);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void filterSet(NGramIndex index, String substring, Set setKeys) {
        int n = 0;
        int ngramSize = index.getNGramSize();
        while(true) {
            String ngram = substring.substring(n, n + ngramSize);
            setKeys.retainAll(index.lookUpNGram(ngram));     
            if (setKeys.isEmpty()) {
                break;
            }
            n += ngramSize;
            if (n == substring.length()) {
                break;
            }
            else if (n + ngramSize > substring.length()) {
                int over = n + ngramSize - substring.length();
                n -= over;
            }
        }
    }

	@Override
	public int calculateEffectiveness(NGramIndex index, String query, Set<Object> keySet, IndexInvocationContext context) {
		if (query.length() > index.getNGramSize()) {
			return query.length() / index.getNGramSize() + 1;
		}
		else {
			// TODO
			return 2 * index.getNGramMap().size();
		}
	}

	@Override
	public void configure(IndexEngineConfig config) {
		// use defaults
	}

	@Override
	public Object createIndexCompatibilityToken(Integer indexConfig) {
		return NGramIndexToken.INSTANCE;
	}

	@Override
	public NGramIndex createIndexInstance(Integer indexConfig) {
		return new NGramIndex(indexConfig);
	}

	@Override
	public boolean evaluate(String query, Object document) {
		String text = String.valueOf(document);
		return text.contains(query);
	}

	@Override
	public void updateIndexEntries(NGramIndex index, Map<Object, IndexUpdateEvent> events, IndexInvocationContext context) {
		for (IndexUpdateEvent event: events.values()) {
			switch (event.getType()) {
			case INSERT:
				index.insert(event.getKey(), String.valueOf(event.getValue()));
				break;
			case UPDATE:
				index.update(event.getKey(), String.valueOf(event.getValue()));
				break;
			case DELETE:
				index.delete(event.getKey());
				break;
			}
		}
	}
}
