package com.lind.depth.resume;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 客户端幂等去重：同一 {@code id} 只接受一次。
 *
 * <p>
 * 对应文档：「至少一次投递」可能重复推送（断点处重叠），客户端按稳定 id 丢弃重复， 收敛成「恰好一次效果」。
 */
public final class ClientEventDeduper {

	private final Set<Long> seen = new HashSet<>();

	/**
	 * @return true 表示首次见到该 id，应展示；false 表示重复，应丢弃
	 */
	public boolean accept(StreamEvent event) {
		Objects.requireNonNull(event, "event");
		return seen.add(event.id());
	}

	public int uniqueCount() {
		return seen.size();
	}

}
