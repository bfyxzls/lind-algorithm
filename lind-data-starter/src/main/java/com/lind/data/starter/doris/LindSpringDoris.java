package com.lind.data.starter.doris;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Apache Doris 场景门面：JDBC 查询 + HTTP Stream Load。
 */
public class LindSpringDoris {

	private final JdbcTemplate jdbcTemplate;

	private final DorisStreamLoad streamLoad;

	public LindSpringDoris(JdbcTemplate jdbcTemplate, DorisStreamLoad streamLoad) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
		this.streamLoad = Objects.requireNonNull(streamLoad, "streamLoad");
	}

	public JdbcTemplate jdbcTemplate() {
		return jdbcTemplate;
	}

	public DorisJdbcQuery jdbc() {
		return new DorisJdbcQuery(jdbcTemplate);
	}

	public DorisStreamLoad streamLoad() {
		return streamLoad;
	}

}
