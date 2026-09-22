package com.lind.algorithm.bloom.ads;

/**
 * 广告频控时间窗口。按小时/天分片写入 Bloom，过期后丢弃整片即可，规避标准 Bloom 无法删除的问题。
 */
public enum FrequencyWindow {

	HOUR, DAY;

	/**
	 * 根据 epoch 毫秒生成分片后缀，例如 {@code h:2026092210}、{@code d:20260922}。
	 */
	public String shardKey(long epochMillis) {
		java.time.Instant instant = java.time.Instant.ofEpochMilli(epochMillis);
		java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneOffset.UTC);
		return switch (this) {
			case HOUR -> String.format("h:%04d%02d%02d%02d", zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
					zdt.getHour());
			case DAY -> String.format("d:%04d%02d%02d", zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth());
		};
	}

}
