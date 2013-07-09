package org.gridkit.coherence.search.bench;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.vicluster.ViExecutor;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

@SuppressWarnings("serial")
public class TradePutTask implements Runnable, Serializable {

	private String cacheName;
	private long from;
	private long to;
	private long batch = 100;
	private TestDataGenerator generator;
	
	public TradePutTask(String cacheName, TestDataGenerator gen, long from, long to) {
		this.cacheName = cacheName;
		this.generator = gen;
		this.from = from;
		this.to = to;
	}

	@Override
	public void run() {
		NamedCache cache = CacheFactory.getCache(cacheName);
		Map<Object, TradeData> batch = new HashMap<Object, TradeData>();
		long i = from;
		while(i < to) {
			TradeData td = new TradeData(generator.getDoc(i));
			++i;
			batch.put(td.getId(), td);
			if (batch.size() >= this.batch) {
				cache.putAll(batch);
				batch.clear();
			}
		}
		if (!batch.isEmpty()) {
			cache.putAll(batch);
			batch.clear();			
		}
	}

	public void parallelRun(Collection<? extends ViExecutor> targets, int split) throws InterruptedException, ExecutionException {
		List<TradePutTask> tasks = new ArrayList<TradePutTask>();
		long n = from;
		while(n < to) {
			long s = n;
			long t = n += split;
			t = Math.min(to, t);
			
			TradePutTask subtask = new TradePutTask(cacheName, generator, s, t);
			tasks.add(subtask);			
		}
		
		final int total = tasks.size();
		final AtomicInteger taskC = new AtomicInteger();
		final Queue<TradePutTask> queue = new ArrayBlockingQueue<TradePutTask>(tasks.size(), false, tasks);
		
		List<Future<Void>> futures = new ArrayList<Future<Void>>();		
		for(final ViExecutor target: targets) {
			final FutureTask<Void> runner = new FutureTask<Void>(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					while(true) {
						TradePutTask task = queue.poll();
						if (task != null) {
							target.exec(task);
							int n = taskC.incrementAndGet();
							System.out.println("Upload progress " + (100d * n / total) + "%");
						}
						else {
							return null;
						}
					}
				}
			});

			futures.add(runner);
			Thread t = new Thread(runner);
			t.start();			
		}
		
		for(Future<Void> f: futures) {
			f.get();
		}
	}
}
