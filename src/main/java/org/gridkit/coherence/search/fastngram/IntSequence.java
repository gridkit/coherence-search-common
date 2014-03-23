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

import java.util.Iterator;

/**
 * {@link Iterator} like interface for 32 bit integers. 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface IntSequence {

    /**
     * Initially sequence could be not in valid state.
     * <br/>
     * Use {@link #advance()} to position to a value. 
     * @return if <code>true</code> you can call {@link #getValue()} 
     */
	public boolean isValid();
	
	/**
	 * Advances to next value.
	 * @return <code>false</code> if no more values
	 */
	public boolean advance();
	
	/**
	 * Unlike {@link Iterator} retrival of value does not advance position.
	 * @return value of current element in sequence
	 */
	public int getValue();

}
