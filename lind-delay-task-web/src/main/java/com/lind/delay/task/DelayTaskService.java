package com.lind.delay.task;

import com.lind.algorithm.wheel.HashedWheelTimer;
import com.lind.algorithm.wheel.Timeout;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 先写库再入轮；到期 claim 后执行；成功 DONE。
 * <p>
 * 真实执行点 = {@code createdAt + delayMs}（写入时固化为 {@code executeAt}）。 重启灌轮：剩余时间 =
 * {@code max(0, executeAt - now)}，禁止「现在 + delayMs」。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DelayTaskService {

	private static final long RETRY_DELAY_MS = 5_000L;

	private final DelayTaskStore store;

	private final HashedWheelTimer timer;

	private final DelayTaskHandlerRegistry handlers;

	private final Map<String, Timeout> localTimeouts = new ConcurrentHashMap<>();

	/**
	 * @param delayMs 相对创建时间的延时；库中同时保存 delayMs 与绝对 executeAt
	 */
	public String schedule(String bizType, String bizId, long delayMs, String payload) {
		if (delayMs < 0) {
			throw new IllegalArgumentException("delayMs must be >= 0");
		}
		long createdAt = System.currentTimeMillis();
		long executeAt = createdAt + delayMs;
		DelayTask task = new DelayTask();
		task.setId(UUID.randomUUID().toString().replace("-", ""));
		task.setBizType(bizType);
		task.setBizId(bizId);
		task.setPayload(payload == null ? "" : payload);
		task.setDelayMs(delayMs);
		task.setExecuteAt(executeAt);
		task.setStatus(DelayTaskStatus.PENDING);
		task.setVersion(0);
		task.setCreatedAt(createdAt);
		task.setUpdatedAt(createdAt);
		store.insert(task);
		offerToWheel(task);
		return task.getId();
	}

	public boolean cancel(String taskId) {
		boolean ok = store.markCancelled(taskId);
		Timeout timeout = localTimeouts.remove(taskId);
		if (timeout != null) {
			timeout.cancel();
		}
		return ok;
	}

	public boolean delete(String taskId) {
		Timeout timeout = localTimeouts.remove(taskId);
		if (timeout != null) {
			timeout.cancel();
		}
		store.markCancelled(taskId);
		return store.delete(taskId);
	}

	/**
	 * 启动灌轮：按库中绝对 executeAt 计算剩余时间。 对未重试过的 PENDING，可用 createdAt+delayMs 校正 executeAt。
	 */
	public int reloadFromStore() {
		List<DelayTask> tasks = store.findReloadable();
		int count = 0;
		for (DelayTask task : tasks) {
			if (task.getStatus() == DelayTaskStatus.RUNNING) {
				long executeAt = task.getExecuteAt();
				store.reschedule(task.getId(), executeAt, Math.max(0L, executeAt - task.getCreatedAt()));
				task.setStatus(DelayTaskStatus.PENDING);
			}
			else if (task.getStatus() == DelayTaskStatus.PENDING) {
				// 用创建时间 + 延时校正真实执行点（避免只存相对 delay 导致重启后语义漂移）
				long expected = task.resolveExecuteAtFromCreate();
				if (expected != task.getExecuteAt() && task.getVersion() == 0) {
					task.setExecuteAt(expected);
				}
			}
			offerToWheel(task);
			count++;
		}
		log.info("reloaded {} delay tasks into wheel", count);
		return count;
	}

	public PageResult<DelayTask> page(int page, int size, String status) {
		return store.page(page, size, status);
	}

	void onFire(String taskId) {
		localTimeouts.remove(taskId);
		boolean claimed = store.casStatus(taskId, DelayTaskStatus.PENDING, DelayTaskStatus.RUNNING);
		if (!claimed) {
			log.debug("skip fire, claim failed: {}", taskId);
			return;
		}
		DelayTask task = store.findById(taskId).orElse(null);
		if (task == null) {
			return;
		}
		DelayTaskHandler handler = handlers.get(task.getBizType());
		if (handler == null) {
			log.error("no handler for bizType={}, mark DEAD id={}", task.getBizType(), taskId);
			store.markDead(taskId);
			return;
		}
		try {
			handler.handle(task);
			store.markDone(taskId);
		}
		catch (Exception e) {
			log.warn("delay task failed, will retry: id={}", taskId, e);
			long nextExecuteAt = System.currentTimeMillis() + RETRY_DELAY_MS;
			if (store.reschedule(taskId, nextExecuteAt, RETRY_DELAY_MS)) {
				store.findById(taskId).ifPresent(this::offerToWheel);
			}
			else {
				store.markDead(taskId);
			}
		}
	}

	/**
	 * 入轮剩余时间 = 绝对执行点 - 当前时间；已过期则立即触发（delay=0）。
	 */
	private void offerToWheel(DelayTask task) {
		long remainingMs = Math.max(0L, task.getExecuteAt() - System.currentTimeMillis());
		String taskId = task.getId();
		Timeout previous = localTimeouts.remove(taskId);
		if (previous != null) {
			previous.cancel();
		}
		log.debug("offerToWheel id={} executeAt={} remainingMs={} (createdAt={} delayMs={})", taskId,
				task.getExecuteAt(), remainingMs, task.getCreatedAt(), task.getDelayMs());
		Timeout timeout = timer.newTimeout(t -> onFire(taskId), remainingMs, TimeUnit.MILLISECONDS);
		localTimeouts.put(taskId, timeout);
	}

}
