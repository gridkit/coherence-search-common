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

package org.gridkit.coherence.misc.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map.Entry;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.EntryFilter;
import com.tangosol.util.filter.IndexAwareFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.PartitionedFilter;

/**
 * Simple filter wrapper which will disable access to index map
 * for nested {@link IndexAwareFilter}.
 * <br/>
 * This filter should be used in it is desirable to disallow
 * index access for certain filter or part of it.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NoIndexFilter implements EntryFilter, PortableObject, Serializable {

	private static final long serialVersionUID = 20130707L;

	public static Filter wrap(Filter filter) {
		if (filter instanceof PartitionedFilter) {
			PartitionedFilter pf = (PartitionedFilter) filter;
			return new PartitionedFilter(wrap(pf.getFilter()), pf.getPartitionSet());
		}
		else if (filter instanceof KeyAssociatedFilter) {
			KeyAssociatedFilter kaf = (KeyAssociatedFilter) filter;
			return new KeyAssociatedFilter(wrap(kaf.getFilter()), kaf.getHostKey());
		}
		else {
			return new NoIndexFilter(filter); 
		}
	}
	
	private Filter filter;
	
	/**
	 * @deprecated Intended to be used by serialization
	 */
	public NoIndexFilter() {		
	}

	protected NoIndexFilter(Filter filter) {
		this.filter = filter;
	}

	@Override
	public boolean evaluate(Object val) {
		return filter.evaluate(val);
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public boolean evaluateEntry(Entry entry) {
		if (filter instanceof EntryFilter) {
			return ((EntryFilter)filter).evaluateEntry(entry);
		}
		else {
			return filter.equals(entry.getValue());
		}
	}

	@Override
	public void readExternal(PofReader in) throws IOException {
		filter = (Filter) in.readObject(1);		
	}

	@Override
	public void writeExternal(PofWriter out) throws IOException {
		out.writeObject(1, filter);		
	}
}
