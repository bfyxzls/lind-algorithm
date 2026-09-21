package com.lind.delay.task;

import java.util.List;
import java.util.Optional;

public interface DelayTaskStore {

	void insert(DelayTask task);

	Optional<DelayTask> findById(String id);

	List<DelayTask> findReloadable();

	PageResult<DelayTask> page(int page, int size, String status);

	boolean casStatus(String id, DelayTaskStatus from, DelayTaskStatus to);

	boolean markDone(String id);

	boolean markCancelled(String id);

	boolean reschedule(String id, long nextExecuteAt, long nextDelayMs);

	boolean markDead(String id);

	boolean delete(String id);

}
