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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Reference implementation of custom index.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class NGramRefList {
    final String ngram;
    final Map<Object, int[]> references = new HashMap<Object, int[]>();
    
    public NGramRefList(String ngarm) {
        this.ngram = ngarm;
    }

    public void addRef(Object key, int position) {
        int[] pList = references.get(key);
        if (pList == null) {
            pList = new int[]{position};
        }
        else {
            pList = Arrays.copyOf(pList, pList.length + 1);
            pList[pList.length - 1] = position;
        }
        references.put(key, pList);
    }
}
