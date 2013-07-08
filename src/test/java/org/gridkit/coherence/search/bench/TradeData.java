package org.gridkit.coherence.search.bench;

import java.util.HashMap;
import java.util.Map;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

@Portable
public class TradeData {

	public static String ID = "ID";
	public static String STATUS = "STATUS";
	public static String SIDE = "SIDE";
	public static String TICKER = "TICKER";
	public static String TRADER = "TRADER";
	public static String CLIENT = "CLIENT";
	public static String TAG = "TAG";
	
	@PortableProperty(1)
	private String id;

	@PortableProperty(2)
	private String status;

	@PortableProperty(3)
	private String side;

	@PortableProperty(4)
	private String ticker;

	@PortableProperty(5)
	private String trader;

	@PortableProperty(6)
	private String client;

	@PortableProperty(7)
	private String tag;
	
	@PortableProperty(8)
	private Map<String, String> garbage = new HashMap<String, String>();
	
	/**
	 * @deprecated To be used by POF serialization
	 */
	public TradeData() {
	}

	public TradeData(Map<String, String> fields) {
		this.garbage = fields;
		this.id = fields.get(ID);
		this.status = fields.get(STATUS);
		this.side = fields.get(SIDE);
		this.ticker = fields.get(TICKER);
		this.trader = fields.get(TRADER);
		this.client = fields.get(CLIENT);
		this.tag = fields.get(TAG);
	}

	public String getId() {
		return id;
	}

	public String getStatus() {
		return status;
	}

	public String getSide() {
		return side;
	}

	public String getTicker() {
		return ticker;
	}

	public String getTrader() {
		return trader;
	}

	public String getClient() {
		return client;
	}

	public String getTag() {
		return tag;
	}
	
	@Override
	public String toString() {
		return garbage.toString();
	}
}
