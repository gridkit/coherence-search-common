package org.gridkit.coherence.search.bench;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.gridkit.coherence.chtest.CacheConfig;
import org.gridkit.coherence.chtest.CacheConfig.DistributedScheme;
import org.gridkit.coherence.chtest.DisposableCohCloud;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.InFilter;

public class FilterPerformanceMicrobench {

	@Rule
	public DisposableCohCloud cloud = new DisposableCohCloud();
	
	@Before
	public void setupCluster() {
		DistributedScheme scheme = CacheConfig.distributedSheme();
		scheme.backingMapScheme(CacheConfig.localScheme());
		
		cloud.all().presetFastLocalCluster();
		cloud.all().pofEnabled(true);
		cloud.all().pofConfig("benchmark-pof-config.xml");
		cloud.all().mapCache("data", scheme);
		cloud.node("storage*").localStorage(true);
		cloud.node("client").localStorage(false);
		cloud.node("storage*").outOfProcess(true);
	}
	
	
	public void initCacheData(int storages, TestDataGenerator generator) throws InterruptedException, ExecutionException {
		List<ViExecutor> targets = new ArrayList<ViExecutor>();
		for(int i = 0; i != storages; ++i) {
			targets.add(cloud.node("storage-" + i));
		}
		cloud.node("storage*").getCache("data");
		TradePutTask task = new TradePutTask("data", generator, 0, generator.getDocCount());
		task.parallelRun(targets, 10000);
	}
	
	public TestDataGenerator configureGenerator(int size) {
		TestDataGenerator tg = new TestDataGenerator();
		tg.setDocCount(size);
		tg.addField(TradeData.ID, 0.1);
		tg.addField(TradeData.SIDE, size / 2);
		tg.addField(TradeData.STATUS, size / 4);
		tg.addField(TradeData.TICKER, 10000);
		tg.addField(TradeData.TRADER, size / 100);
		tg.addField(TradeData.CLIENT, 1000);
		tg.addField(TradeData.TAG, 0.8);
		return tg;
	}
	
	@Test
	public void validate_cluster_set_up() throws InterruptedException, ExecutionException {
		TestDataGenerator tg = configureGenerator(1000000);
		JvmProps.addJvmArg(cloud.node("storage-*"), "|-Xmx1024m|-Xms1024m|-XX:+UseConcMarkSweepGC");
		
		initCacheData(2, tg);
		
		cloud.node("client").exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				System.out.println("Cache size: " + CacheFactory.getCache("data").size());
				return null;
			}
		});
	}

	@Test
	public void verify_full_vs_index_scan() throws InterruptedException, ExecutionException {
		TestDataGenerator tg = configureGenerator(10000000);
		JvmProps.addJvmArg(cloud.node("storage-*"), "|-Xmx1024m|-Xms1024m|-XX:+UseConcMarkSweepGC");
		
		initCacheData(2, tg);
		
		final Filter tagFilter = tagFilter(tg, 1, 10);
		
		cloud.node("client").exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				NamedCache cache = CacheFactory.getCache("data");
				System.out.println("Cache size: " + cache.size());
				calculate_query_time(tagFilter);
				long time;
				time = calculate_query_time(tagFilter);
				System.out.println("Exec time for [tagFilter] no index - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));
				
				cache.addIndex(new ReflectionExtractor("getTag"), false, null);
				calculate_query_time(tagFilter);
				
				time = calculate_query_time(tagFilter);
				System.out.println("Exec time for [tagFilter] with index - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));
				
				return null;
			}
		});
	}

	@Test
	public void verify_negative_index_impact() throws InterruptedException, ExecutionException {
		TestDataGenerator tg = configureGenerator(1000000);
		JvmProps.addJvmArg(cloud.node("storage-*"), "|-Xmx1024m|-Xms1024m|-XX:+UseConcMarkSweepGC");
		
		initCacheData(2, tg);
		
		final Filter tagFilter = tagFilter(tg, 1, 10);
		final Filter sideFilter = sideFilter(tg, 1);

		final Filter sideAndTagFilter = new AndFilter(sideFilter, tagFilter);
		final Filter tagAndSideFilter = new AndFilter(tagFilter, sideFilter);
		
		cloud.node("client").exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				NamedCache cache = CacheFactory.getCache("data");
				System.out.println("Cache size: " + cache.size());

				long time;
				time = calculate_query_time(sideAndTagFilter);
				System.out.println("Exec time for [sideAndTagFilter] no index - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				time = calculate_query_time(tagAndSideFilter);
				System.out.println("Exec time for [tagAndSideFilter] no index - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));
				
				cache.addIndex(new ReflectionExtractor("getTag"), false, null);

				time = calculate_query_time(sideAndTagFilter);
				System.out.println("Exec time for [sideAndTagFilter] , index by tag - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				time = calculate_query_time(tagAndSideFilter);
				System.out.println("Exec time for [tagAndSideFilter] , index by tag - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				cache.addIndex(new ReflectionExtractor("getSide"), false, null);
				
				time = calculate_query_time(sideAndTagFilter);
				System.out.println("Exec time for [sideAndTagFilter] , index by tag & side - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));
				
				time = calculate_query_time(tagAndSideFilter);
				System.out.println("Exec time for [tagAndSideFilter] , index by tag & side - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				return null;
			}
		});
	}

	
	@Test
	public void verify_no_index_hint_impact() throws InterruptedException, ExecutionException {
		TestDataGenerator tg = configureGenerator(1000000);
		JvmProps.addJvmArg(cloud.node("storage-*"), "|-Xmx1024m|-Xms1024m|-XX:+UseConcMarkSweepGC");
		
		initCacheData(2, tg);
		
		final Filter tickerFilter = tickerFilter(tg, 1);
		final Filter sideFilter = sideFilter(tg, 1);

		final Filter sideAndTickerFilter = new AndFilter(sideFilter, tickerFilter);
		final Filter tickerAndSideFilter = new AndFilter(tickerFilter, sideFilter);
		
		cloud.node("client").exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				NamedCache cache = CacheFactory.getCache("data");
				System.out.println("Cache size: " + cache.size());

				long time;
				time = calculate_query_time(sideAndTickerFilter);
				System.out.println("Exec time for [sideAndTickerFilter] no index - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				time = calculate_query_time(tickerAndSideFilter);
				System.out.println("Exec time for [tickerAndSideFilter] no index - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));
				
				cache.addIndex(new ReflectionExtractor("getTag"), false, null);

				time = calculate_query_time(sideAndTickerFilter);
				System.out.println("Exec time for [sideAndTickerFilter] , index by tag - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				time = calculate_query_time(tickerAndSideFilter);
				System.out.println("Exec time for [tickerAndSideFilter] , index by tag - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				cache.addIndex(new ReflectionExtractor("getSide"), false, null);
				
				time = calculate_query_time(sideAndTickerFilter);
				System.out.println("Exec time for [sideAndTickerFilter] , index by tag & side - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));
				
				time = calculate_query_time(tickerAndSideFilter);
				System.out.println("Exec time for [tickerAndSideFilter] , index by tag & side - " + 0.001 * TimeUnit.NANOSECONDS.toMicros(time));

				return null;
			}
		});
	}
	
	private InFilter tagFilter(TestDataGenerator tg, long seed, int size) {
		Random rnd = new Random(seed);
		HashSet<String> terms = new HashSet<String>();
		for(int i = 0; i != size; ++i) {
			terms.add(tg.getRandomTerm(rnd, "TAG"));
		}
		return new InFilter("getTag", terms);
	}

	private EqualsFilter sideFilter(TestDataGenerator tg, long seed) {
		Random rnd = new Random(seed);
		return new EqualsFilter("getSide", tg.getRandomTerm(rnd, "SIDE"));
	}

	private EqualsFilter tickerFilter(TestDataGenerator tg, long seed) {
		Random rnd = new Random(seed);
		return new EqualsFilter("getTicker", tg.getRandomTerm(rnd, "TICKER"));
	}
	
	public static long calculate_query_time(Filter filter) {
		// warm up first
		calculate_query_time2(filter);
		return calculate_query_time2(filter);
	}

	private static long calculate_query_time2(Filter filter) {
		long st = System.nanoTime();
		NamedCache cache = CacheFactory.getCache("data");
		int n = 0;
		while(true) {
			cache.aggregate(filter, new Count());
			n++;
			long time = System.nanoTime() - st;
			if (n > 10 && time > TimeUnit.SECONDS.toNanos(10)) {
				break;
			}
			if (n > 100) {
				break;
			}
		}
		long time = System.nanoTime() - st;
		return time / n;
	}
}
