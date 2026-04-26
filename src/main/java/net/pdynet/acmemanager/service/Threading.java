package net.pdynet.acmemanager.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Threading {
	
	private static ScheduledExecutorService singleThreadingScheduler = null;
	
	public static synchronized ScheduledExecutorService getSingleThreadingScheduler() {
		if (singleThreadingScheduler == null)
			singleThreadingScheduler = Executors.newSingleThreadScheduledExecutor();
		
		return singleThreadingScheduler;
	}
	
	public static synchronized void close() {
		try {
			if (singleThreadingScheduler != null) {
				singleThreadingScheduler.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
