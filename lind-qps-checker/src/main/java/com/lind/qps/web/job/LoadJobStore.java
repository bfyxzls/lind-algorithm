package com.lind.qps.web.job;

import java.util.List;
import java.util.Optional;

/**
 * 压测任务持久化。
 */
public interface LoadJobStore {

	void insert(LoadJob job);

	void updateFinished(LoadJob job);

	Optional<LoadJob> findById(String id);

	List<LoadJob> findRecent(int limit);

	/**
	 * 进程重启后，将残留 RUNNING 任务标记为失败。
	 */
	int markInterruptedRunning(String message);

}
