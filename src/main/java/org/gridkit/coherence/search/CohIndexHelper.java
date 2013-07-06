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

import java.util.Map;

import com.tangosol.util.MapTrigger;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.AbstractExtractor;

public class CohIndexHelper {

	@SuppressWarnings("rawtypes")
	public static Object extractFromEntryOrValue(Map.Entry entry, ValueExtractor extractor) {
		if (extractor instanceof AbstractExtractor) {
			return ((AbstractExtractor)extractor).extractFromEntry(entry);
		}
		else {
			return extractor.extract(entry.getValue());
		}
	}

	public static Object extractFromOriginalValue(MapTrigger.Entry entry, ValueExtractor extractor) {
		if (extractor instanceof AbstractExtractor) {
			return ((AbstractExtractor)extractor).extractOriginalFromEntry(entry);
		}
		else {
			return extractor.extract(entry.getOriginalValue());
		}		
	}
}
