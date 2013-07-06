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

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

/**
 * Default implementation of {@link IndexEngineConfig}. 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class DefaultIndexEngineConfig implements IndexEngineConfig, Serializable, PortableObject {

	private static final long serialVersionUID = 20100814L;
	
	private int indexUpdateQueueSizeLimit = 1024;
	private int indexUpdateDelay = 5000;
	private boolean attributeIndexEnabled = false;
	private boolean oldValueOnUpdateEnabled = false;
	
	public int getIndexUpdateQueueSizeLimit() {
		return indexUpdateQueueSizeLimit;
	}
	
	public void setIndexUpdateQueueSizeLimit(int indexUpdateQueueSizeLimit) {
		this.indexUpdateQueueSizeLimit = indexUpdateQueueSizeLimit;
	}
	
	public int getIndexUpdateDelay() {
		return indexUpdateDelay;
	}
	
	public void setIndexUpdateDelay(int indexUpdateDelay) {
		this.indexUpdateDelay = indexUpdateDelay;
	}
	
	public boolean isAttributeIndexEnabled() {
		return attributeIndexEnabled;
	}
	
	/**
	 * @deprecated Doesn't work well, do not use
	 */
	public void setAttributeIndexEnabled(boolean attributeIndexEnabled) {
		this.attributeIndexEnabled = attributeIndexEnabled;
	}
	
	public boolean isOldValueOnUpdateEnabled() {
		return oldValueOnUpdateEnabled;
	}

	public void setOldValueOnUpdateEnabled(boolean oldValueOnUpdateEnabled) {
		this.oldValueOnUpdateEnabled = oldValueOnUpdateEnabled;
	}

	@Override
	public void readExternal(PofReader in) throws IOException {
		int i = 1;
		attributeIndexEnabled = in.readBoolean(i++);
		oldValueOnUpdateEnabled = in.readBoolean(i++);
		indexUpdateDelay = in.readInt(i++);
		indexUpdateQueueSizeLimit = in.readInt(i++);
	}

	@Override
	public void writeExternal(PofWriter out) throws IOException {
		int i = 1;
		out.writeBoolean(i++, attributeIndexEnabled);
		out.writeBoolean(i++, oldValueOnUpdateEnabled);
		out.writeInt(i++, indexUpdateDelay);
		out.writeInt(i++, indexUpdateQueueSizeLimit);
	}
}
