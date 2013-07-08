package org.gridkit.coherence.search.bench;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("serial")
public class TestDataGenerator implements Serializable {

	Map<String, FieldDescription> fields = new LinkedHashMap<String, TestDataGenerator.FieldDescription>();
	
	int documentCount = 100000;
	
	public int getDocCount() {
		return documentCount;
	}
	
	public void setDocCount(int docs) {
		documentCount = docs;
	}
	
	public List<String> getFieldList() {
		return new ArrayList<String>(fields.keySet());
	}
	
	public void addField(String field, double selectivity) {
		fields.put(field, new FieldDescription(field, selectivity));
	}
	
	public double getSelectivity(String field) {
		return fields.get(field).selectivity;
	}
	
	public Map<String, String> getDoc(long id) {
		Random rnd = new Random(id);
		Map<String, String> map = new HashMap<String, String>();
		map.put("ID", String.valueOf(id));
		for(FieldDescription fd: fields.values()) {
			String fieldName = fd.fieldName;
			double range = documentCount / fd.selectivity;
			long sn = (long)(Math.abs(rnd.nextDouble()) * range);
			map.put(fieldName, getTerm(fieldName, sn));
		}
		return map;
	}
	
	public String getRandomTerm(Random rnd, String field) {
		FieldDescription fd = fields.get(field);
		double range = documentCount / fd.selectivity;
		return getTerm(field, (long)(Math.abs(rnd.nextDouble()) * range));
	}

	public String[] getRandomRange(Random rnd, String field, int len) {
		FieldDescription fd = fields.get(field);
		double range = documentCount / fd.selectivity;
		String[] r = new String[2];
		long low = (long)(Math.abs(rnd.nextDouble()) * range);
		r[0] = getTerm(field, low);
		r[1] = getTerm(field, low + len);
		
		return r;
	}
	
	public String getTerm(String field, long sn) {
		Random rnd = new Random(field.hashCode() ^ sn);
		int len = rnd.nextInt(16) + 1;
		char[] buf = new char[len];
		for(int i = 0; i != len; ++i) {
			buf[i] = (char) ('A' + rnd.nextInt(26));
		}
		long n = 0x40000000 | sn;
		return Long.toHexString(n) + "-" + new String(buf);
	}
	
	private static class FieldDescription implements Serializable {
		
		String fieldName;
		double selectivity;
		
		public FieldDescription(String fieldName, double selectivity) {
			this.fieldName = fieldName;
			this.selectivity = selectivity;
		}
	}
}
