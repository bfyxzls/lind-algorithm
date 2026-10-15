package com.lind.data.starter.doris;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * 基于 {@link JdbcTemplate} 的 Doris SQL 场景（MySQL 协议连 FE Query Port）。
 */
public class DorisJdbcQuery {

	private final JdbcTemplate jdbc;

	public DorisJdbcQuery(JdbcTemplate jdbc) {
		this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
	}

	public List<Map<String, Object>> queryForList(String sql, Object... args) {
		return jdbc.queryForList(sql, args);
	}

	public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
		if (args == null || args.length == 0) {
			return jdbc.query(sql, rowMapper);
		}
		return jdbc.query(sql, rowMapper, args);
	}

	public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
		if (args == null || args.length == 0) {
			return jdbc.queryForObject(sql, requiredType);
		}
		return jdbc.queryForObject(sql, requiredType, args);
	}

	public int update(String sql, Object... args) {
		if (args == null || args.length == 0) {
			return jdbc.update(sql);
		}
		return jdbc.update(sql, args);
	}

	public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
		Objects.requireNonNull(batchArgs, "batchArgs");
		return jdbc.batchUpdate(sql, batchArgs);
	}

	public JdbcTemplate template() {
		return jdbc;
	}

}
