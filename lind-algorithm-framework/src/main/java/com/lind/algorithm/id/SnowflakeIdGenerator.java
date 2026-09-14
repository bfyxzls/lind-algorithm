package com.lind.algorithm.id;

/**
 * 雪花算法 ID 生成器：1 符号位 + 41 时间戳 + 10 工作节点 + 12 序列。
 */
public final class SnowflakeIdGenerator {

	private static final long EPOCH = 1_704_067_200_000L; // 2024-01-01 UTC

	private static final long WORKER_ID_BITS = 10L;

	private static final long SEQUENCE_BITS = 12L;

	private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

	private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

	private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

	private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

	private final long workerId;

	private long lastTimestamp = -1L;

	private long sequence;

	public SnowflakeIdGenerator(long workerId) {
		if (workerId < 0 || workerId > MAX_WORKER_ID) {
			throw new IllegalArgumentException("workerId out of range: 0.." + MAX_WORKER_ID);
		}
		this.workerId = workerId;
	}

	public synchronized long nextId() {
		long timestamp = currentTime();
		if (timestamp < lastTimestamp) {
			throw new IllegalStateException("clock moved backwards: " + (lastTimestamp - timestamp) + "ms");
		}
		if (timestamp == lastTimestamp) {
			sequence = (sequence + 1) & SEQUENCE_MASK;
			if (sequence == 0L) {
				timestamp = waitNextMillis(lastTimestamp);
			}
		}
		else {
			sequence = 0L;
		}
		lastTimestamp = timestamp;
		return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
	}

	public long workerId() {
		return workerId;
	}

	private long waitNextMillis(long last) {
		long ts = currentTime();
		while (ts <= last) {
			ts = currentTime();
		}
		return ts;
	}

	private long currentTime() {
		return System.currentTimeMillis();
	}

}
