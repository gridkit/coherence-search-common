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

public class FastNGramIndexTest {

    public static class Fast2x6IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new FastNGramIndex(2, 6);
        }
    }

    public static class Fast2x7IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new FastNGramIndex(2, 7);
        }
    }

    public static class Fast3x6IndexTest extends AbstractNGramIndexTest {

        @Override
        public TextSearchIndex newIndex() {
            return new FastNGramIndex(3, 6);
        }
    }

    public static class Fast3x7IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new FastNGramIndex(3, 7);
        }
    }

    public static class Fast4x5IndexTest extends AbstractNGramIndexTest {
        
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
            return new FastNGramIndex(4, 5);
        }
    }

    public static class Fast4x6IndexTest extends AbstractNGramIndexTest {
        
        @Override
        public TextSearchIndex newIndex() {
            return new FastNGramIndex(4, 6);
        }
    }
    
}
