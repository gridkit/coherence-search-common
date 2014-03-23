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

public class TrieNGramIndexTest {

    public static class Trie2x6IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new TrieNGramIndex(2, 6, 4);
        }
    }

    public static class Trie2x7IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new TrieNGramIndex(2, 7, 4);
        }
    }

    public static class Trie3x6IndexTest extends AbstractNGramIndexTest {

        @Override
        public TextSearchIndex newIndex() {
            return new TrieNGramIndex(3, 6, 4);
        }
    }

    public static class Trie3x7IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new TrieNGramIndex(3, 7, 4);
        }
    }

    public static class Trie4x5IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public void verify_simple_search_cases() {
            // test skipped, 5 bit precision affects candidate set
        }

        @Override
        public void verify_simple_update_cases() {
            // test skipped, 5 bit precision affects candidate set
        }

        @Override
        public TextSearchIndex newIndex() {
            return new TrieNGramIndex(4, 5, 4);
        }
    }

    public static class Trie4x6IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new TrieNGramIndex(4, 6, 4);
        }
    }

    public static class Trie4x7IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new TrieNGramIndex(4, 7, 4);
        }
    }
    
}
