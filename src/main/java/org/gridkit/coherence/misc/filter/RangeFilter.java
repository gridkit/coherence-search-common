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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.ComparisonFilter;
import com.tangosol.util.filter.ExtractorFilter;
import com.tangosol.util.filter.IndexAwareFilter;

/**
 * {@link RangeFilter} is alternative to Coherence {@link BetweenFilter}.
 * It selects objects by range of attributes, bounds of range can be either included or excluded using flags.
 * {@link Comparator} can optionally be provided (otherwise objects should be {@link Comparable} themselves).
 * 
 * {@link RangeFilter} will use index if available. Ordered indexes are preferable, but unordered index can be used also (though it would be slower).
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RangeFilter extends ExtractorFilter implements IndexAwareFilter, Serializable, PortableObject {

	private static final long serialVersionUID = 20110827L;
	
	private Object lower;
	private Object upper;
	private boolean includeLower;
	private boolean includeUpper;
	@SuppressWarnings("rawtypes")
	private Comparator comparator;
	
	/**
	 * @deprecated Requeired by POF, do not use it
	 */
	public RangeFilter() {};
	
	public RangeFilter(ValueExtractor e, Object lower, Object upper, boolean includeLower, boolean includeUpper, Comparator<?> comparator) {
		super(e);
		this.lower = lower;
		this.upper = upper;
		this.includeLower = includeLower;
		this.includeUpper = includeUpper;
		this.comparator = comparator;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean evaluateExtracted(Object o) {
		if (comparator != null) {
			int cl = comparator.compare(lower, o);
			if ((includeLower && cl > 0) || (!includeLower && cl >= 0)) {
				return false;
			}
			int cu = comparator.compare(o, upper);
			if ((includeUpper && cu > 0) || (!includeUpper && cu >= 0)) {
				return false;
			}
			return true;
		}
		else {
			int cl = ((Comparable)lower).compareTo(o);
			if ((includeLower && cl > 0) || (!includeLower && cl >= 0)) {
				return false;
			}
			int cu = ((Comparable)o).compareTo(upper);
			if ((includeUpper && cu > 0) || (!includeUpper && cu >= 0)) {
				return false;
			}
			return true;			
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Filter applyIndex(Map indexMap, Set keySet) {
		Map invMap = getInvertedIndex(indexMap);
		if (invMap == null) {
			return this;
		}
		else if (invMap instanceof SortedMap) {
			SortedMap sortedInvMap = (SortedMap) invMap;
			SortedMap range = sortedInvMap.subMap(lower, upper);
			Collection keys = null;
			boolean first = true;
			boolean open = includeLower;
			for(Object entry: range.entrySet()) {
				Object key = ((Map.Entry)entry).getKey();
				Collection val = (Collection) ((Map.Entry)entry).getValue();
				if (open || evaluateExtracted(key)) {
					open = true;
					if (keys == null) {
						keys = val;
					}
					else if (first) {
						Set tk = new HashSet();
						tk.addAll(keys);
						tk.addAll(val);
						keys = tk;
						first = false;
					}
					else {
						keys.addAll(val);
					}
				}
			}
			if (includeUpper) {
				Collection val = (Collection) invMap.get(upper);
				if (val !=null) {
					if (keys == null) {
						keys = val;
					}
					else if (first) {
						Set tk = new HashSet();
						tk.addAll(keys);
						tk.addAll(val);
						keys = tk;
						first = false;
					} else {
						keys.addAll(val);
					}
				}
			}
			if (keys == null) {
				keySet.clear();
			}
			else {
				keySet.retainAll(keys);
			}
			return null;
		}
		else {
			Set keys = new HashSet();
			for (Object key: invMap.keySet()) {
				if (evaluateExtracted(key)) {
					keys.addAll((Collection) invMap.get(key));
				}
			}
			keySet.retainAll(keys);
			return null;
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int calculateEffectiveness(Map indexMap, Set keySet) {
		Map invMap = getInvertedIndex(indexMap);
		if (invMap == null) {
			return keySet.size() * ComparisonFilter.EVAL_COST;
		}
		else if (invMap instanceof SortedMap) {
			SortedMap sInvMap = (SortedMap) invMap;
			return sInvMap.subMap(lower, upper).size();
		}
		else {
			return invMap.size();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map getInvertedIndex(Map indexMap) {
		MapIndex index = (MapIndex) indexMap.get(m_extractor);
		if (index != null) {
			boolean compatible = false;
			if (index.isOrdered()) {
				Comparator cmp = index.getComparator();
				if (cmp instanceof SafeComparator) {
					cmp = ((SafeComparator)cmp).getComparator();
				}
				if ((comparator == null && cmp == null) || (cmp != null && cmp.equals(comparator))) {
					compatible = true;
				}
			}
			return compatible ? index.getIndexContents() : Collections.unmodifiableMap(index.getIndexContents()); 
		}
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void readExternal(DataInput in) throws IOException {
		super.readExternal(in);
		lower = readObject(in);
		upper = readObject(in);
		includeLower = in.readBoolean();
		includeUpper = in.readBoolean();
		comparator = (Comparator) readObject(in);
	}

	@Override
	public void writeExternal(DataOutput out) throws IOException {
		super.writeExternal(out);
		writeObject(out, lower);
		writeObject(out, upper);
		out.writeBoolean(includeLower);
		out.writeBoolean(includeUpper);
		writeObject(out, comparator);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void readExternal(PofReader in) throws IOException {
		int n = 1;
		PofReader nested = in.createNestedPofReader(n++);
		super.readExternal(nested);
		lower = in.readObject(n++);
		upper = in.readObject(n++);
		includeLower = in.readBoolean(n++);
		includeUpper = in.readBoolean(n++);
		comparator = (Comparator) in.readObject(n++);
	}

	@Override
	public void writeExternal(PofWriter out) throws IOException {
		int n = 1;
		PofWriter nested = out.createNestedPofWriter(n++); 
		super.writeExternal(nested);
		out.writeObject(n++, lower);
		out.writeObject(n++, upper);
		out.writeBoolean(n++, includeLower);
		out.writeBoolean(n++, includeUpper);
		out.writeObject(n++, comparator);
	}
}
