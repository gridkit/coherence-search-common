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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Reference implementation of custom index.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NGramIndex {

    final int ngramSize;
    final Map<String, NGramRefList> index = new HashMap<String, NGramRefList>();
    final Map<Object, String[]> extract = new HashMap<Object, String[]>();  
    
    public NGramIndex(int ngramSize) {
        this.ngramSize = ngramSize;
    }

    public int getNGramSize() {
        return ngramSize;
    }
    
    private String[] split(String string) {
        if (string.length() == 0) {
            return null;
        }
        else if (string.length() <= ngramSize) {
            return new String[]{string};
        }
        else {
            int n = string.length() - ngramSize + 1;
            String[] split = new String[n];
            for(int i = 0; i != n; ++i) {
                split[i] = string.substring(i, i + ngramSize);
            }
            return split;
        }
    }
    
    public void insert(Object key, String text) {
        String[] split = split(text);
        if (split != null) {            
            updateIndex(key, split);
            extract.put(key, normalize(split));
        }
    }

    public void update(Object key, String text) {
    	delete(key);
    	insert(key, text);
    }
    
    public void delete(Object key) {
    	String[] split = extract.get(key);
    	if (split != null) {
    		for(String ngram : split) {
    			removeFromIndex(ngram, key);
    		}
    	}
    }

    private String[] normalize(String[] split) {
        for(int i = 0 ; i != split.length; ++i) {
            NGramRefList list = index.get(split[i]);
            if (list != null) {
                split[i] = list.ngram;
            }
        }
        return split;
    }

    private void updateIndex(Object key, String[] split) {
        for(int i = 0; i != split.length; ++i) {
            insertToIndex(split[i], key, i);
        }
    }


    
    
    private void insertToIndex(String ngram, Object key, int position) {
        NGramRefList list = index.get(ngram);
        if (list == null) {
            list = new NGramRefList(new String(ngram));
            index.put(list.ngram, list);
        }
        
        list.addRef(key, position);
    }
    
    private void removeFromIndex(String ngram, Object key) {
        NGramRefList list = index.get(ngram);
        if (list != null) {
            list.references.remove(key);
            if (list.references.size() == 0) {
                index.remove(ngram);
            }
        }
    }

    @SuppressWarnings("unchecked")
	public Collection<NGramRefList> lookUpNGram(String ngram) {
        NGramRefList refList = index.get(ngram); 
        return refList == null ? Collections.EMPTY_SET : refList.references.keySet();
    }

	public Map<String, NGramRefList> getNGramMap() {
		return index;
	}
}
