package com.lind.qps.web.job;

import com.lind.qps.engine.CapacityFinder;
import com.lind.qps.engine.LoadConfig;
import com.lind.qps.engine.LoadReport;
import com.lind.qps.engine.LoadRunner;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/**
 * 异步压测任务编排：创建与结束后持久化到 H2，运行中保留内存态便于轮询。
 */
@Service
public class LoadJobService {

	private static final int HISTORY_LIMIT = 200;

	private final LoadJobStore store;

	private final Map<String, LoadJob> runningJobs = new ConcurrentHashMap<>();

	private final AtomicInteger runningCount = new AtomicInteger();

	private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "qps-load-job");
		t.setDaemon(true);
		return t;
	});

	public LoadJobService(LoadJobStore store) {
		this.store = store;
	}

	@PostConstruct
	void recoverInterruptedJobs() {
		store.markInterruptedRunning("进程重启，压测任务中断");
	}

	public LoadJob submit(LoadForm form) {
		LoadMode mode = form.getMode() == null ? LoadMode.FIXED : form.getMode();
		LoadConfig config = LoadFormMapper.toLoadConfig(form);
		if (mode == LoadMode.FIND_MAX) {
			config = config.toBuilder().duration(Duration.ofSeconds(Math.max(1, form.getDurationSeconds()))).build();
		}
		LoadJob job = new LoadJob(UUID.randomUUID().toString().replace("-", "").substring(0, 12), mode, config);
		if (mode == LoadMode.FIND_MAX) {
			job.startConcurrency(form.getStartConcurrency());
			job.step(form.getStep());
			job.maxConcurrency(form.getMaxConcurrency());
			job.minSuccessRatePercent(form.getMinSuccessRatePercent());
			job.maxAllowedLatencyMillis(form.getMaxAllowedLatencyMillis());
		}
		job.status(LoadJobStatus.RUNNING);
		store.insert(job);
		runningJobs.put(job.id(), job);
		runningCount.incrementAndGet();
		executor.execute(() -> execute(job));
		return job;
	}

	public Optional<LoadJob> find(String id) {
		LoadJob running = runningJobs.get(id);
		if (running != null) {
			return Optional.of(running);
		}
		return store.findById(id);
	}

	public List<LoadJob> list() {
		return store.findRecent(HISTORY_LIMIT);
	}

	public int runningCount() {
		return runningCount.get();
	}

	private void execute(LoadJob job) {
		try {
			if (job.mode() == LoadMode.FIND_MAX) {
				CapacityFinder.CapacityResult result = CapacityFinder.builder(job.config())
						.startConcurrency(job.startConcurrency()).step(job.step()).maxConcurrency(job.maxConcurrency())
						.minSuccessRatePercent(job.minSuccessRatePercent())
						.maxAllowedLatencyMillis(job.maxAllowedLatencyMillis()).build().find();
				job.capacityResult(result);
				job.report(result.bestPassingReport() != null ? result.bestPassingReport() : result.lastReport());
				job.message("最大可用并发=" + result.maxConcurrency());
			}
			else {
				LoadReport report = new LoadRunner(job.config()).run();
				job.report(report);
				job.message(String.format("QPS=%.2f, 成功率=%.2f%%", report.qps(), report.successRate()));
			}
			job.status(LoadJobStatus.SUCCESS);
		}
		catch (Exception ex) {
			job.status(LoadJobStatus.FAILED);
			job.message(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
		}
		finally {
			job.finishedAt(Instant.now());
			try {
				store.updateFinished(job);
			}
			finally {
				runningJobs.remove(job.id());
				runningCount.decrementAndGet();
			}
		}
	}

}
