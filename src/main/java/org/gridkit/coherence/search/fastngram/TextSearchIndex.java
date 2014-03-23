/**
 * Copyright 2014 Alexey Ragozin
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


/**
 * This interface is used to implement full text search alike indexes.
 * Index structure encapsulated in this interface does not hold
 * text content, so it cannot provide exact answer without knowing
 * text value.
 * <br/>
 * Instead, index offers a candidate set and separate query evaluation method.
 * <br/>
 * Code using {@link TextSearchIndex} is expected to implement final filtering
 * of candidate set returned by index structure. 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface TextSearchIndex {

	/**
	 * Though, candidate set is inaccurate in general. Some specific queries
	 * may produce accurate candidates set and do not require post filtering.
	 * <br/>
	 * E.g. for n-gram index with n=3, substring query with length 3 and below 
	 * would produce accurate candidate sets.
	 * 
	 * @param query
	 * @return <code>true</code> if post filtering is not required for query.
	 */
	public boolean isIndexOnly(String query);

	/**
	 * Evaluate text string against query string using semantic of index.
	 * @param text
	 * @param query
	 * @return
	 */
	public boolean evaluate(String text, String query);
	
	/**
	 * Test row against candidate set produced by query 
	 * @return <code>true</code> if row is a candidate for query
	 */
	public boolean screen(int row, String query);

    /**
     * @return sequence of rows in candidate set
     */
	public IntSequence getCanditates(String query);
	
    /**
     * Add row to index
     */
	public void addRow(int rowId, String text);
	
    /**
     * Remove row from index, text should match text 
     */
	public void removeRow(int rowId, String text);
	
}
