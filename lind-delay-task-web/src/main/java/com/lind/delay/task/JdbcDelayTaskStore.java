package com.lind.delay.task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JdbcDelayTaskStore implements DelayTaskStore {

	private static final RowMapper<DelayTask> ROW_MAPPER = JdbcDelayTaskStore::mapRow;

	private final JdbcTemplate jdbc;

	public JdbcDelayTaskStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void insert(DelayTask task) {
		jdbc.update("""
				INSERT INTO delay_task
				(id, biz_type, biz_id, payload, delay_ms, execute_at, status, version, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""", task.getId(), task.getBizType(), task.getBizId(), task.getPayload(), task.getDelayMs(),
				task.getExecuteAt(), task.getStatus().name(), task.getVersion(), task.getCreatedAt(),
				task.getUpdatedAt());
	}

	@Override
	public Optional<DelayTask> findById(String id) {
		List<DelayTask> list = jdbc.query("SELECT * FROM delay_task WHERE id = ?", ROW_MAPPER, id);
		return list.stream().findFirst();
	}

	@Override
	public List<DelayTask> findReloadable() {
		return jdbc.query("""
				SELECT * FROM delay_task
				WHERE status IN ('PENDING', 'RUNNING')
				ORDER BY execute_at ASC
				""", ROW_MAPPER);
	}

	@Override
	public PageResult<DelayTask> page(int page, int size, String status) {
		int safePage = Math.max(1, page);
		int safeSize = Math.min(100, Math.max(1, size));
		int offset = (safePage - 1) * safeSize;

		boolean filter = StringUtils.hasText(status);
		Long total = filter
				? jdbc.queryForObject("SELECT COUNT(*) FROM delay_task WHERE status = ?", Long.class, status)
				: jdbc.queryForObject("SELECT COUNT(*) FROM delay_task", Long.class);
		long totalCount = total == null ? 0L : total;

		List<DelayTask> items;
		if (filter) {
			items = jdbc.query("""
					SELECT * FROM delay_task WHERE status = ?
					ORDER BY created_at DESC
					LIMIT ? OFFSET ?
					""", ROW_MAPPER, status, safeSize, offset);
		}
		else {
			items = jdbc.query("""
					SELECT * FROM delay_task
					ORDER BY created_at DESC
					LIMIT ? OFFSET ?
					""", ROW_MAPPER, safeSize, offset);
		}
		return new PageResult<>(items == null ? List.of() : items, safePage, safeSize, totalCount);
	}

	@Override
	public boolean casStatus(String id, DelayTaskStatus from, DelayTaskStatus to) {
		long now = System.currentTimeMillis();
		int rows = jdbc.update("""
				UPDATE delay_task
				SET status = ?, version = version + 1, updated_at = ?
				WHERE id = ? AND status = ?
				""", to.name(), now, id, from.name());
		return rows == 1;
	}

	@Override
	public boolean markDone(String id) {
		return casStatus(id, DelayTaskStatus.RUNNING, DelayTaskStatus.DONE)
				|| casStatus(id, DelayTaskStatus.PENDING, DelayTaskStatus.DONE);
	}

	@Override
	public boolean markCancelled(String id) {
		long now = System.currentTimeMillis();
		int rows = jdbc.update("""
				UPDATE delay_task
				SET status = ?, version = version + 1, updated_at = ?
				WHERE id = ? AND status IN ('PENDING', 'RUNNING')
				""", DelayTaskStatus.CANCELLED.name(), now, id);
		return rows == 1;
	}

	@Override
	public boolean reschedule(String id, long nextExecuteAt, long nextDelayMs) {
		long now = System.currentTimeMillis();
		int rows = jdbc.update("""
				UPDATE delay_task
				SET status = ?, execute_at = ?, delay_ms = ?, version = version + 1, updated_at = ?
				WHERE id = ? AND status IN ('PENDING', 'RUNNING')
				""", DelayTaskStatus.PENDING.name(), nextExecuteAt, nextDelayMs, now, id);
		return rows == 1;
	}

	@Override
	public boolean markDead(String id) {
		long now = System.currentTimeMillis();
		int rows = jdbc.update("""
				UPDATE delay_task
				SET status = ?, version = version + 1, updated_at = ?
				WHERE id = ?
				""", DelayTaskStatus.DEAD.name(), now, id);
		return rows == 1;
	}

	@Override
	public boolean delete(String id) {
		return jdbc.update("DELETE FROM delay_task WHERE id = ?", id) == 1;
	}

	private static DelayTask mapRow(ResultSet rs, int rowNum) throws SQLException {
		DelayTask task = new DelayTask();
		task.setId(rs.getString("id"));
		task.setBizType(rs.getString("biz_type"));
		task.setBizId(rs.getString("biz_id"));
		task.setPayload(rs.getString("payload"));
		task.setDelayMs(rs.getLong("delay_ms"));
		task.setExecuteAt(rs.getLong("execute_at"));
		task.setStatus(DelayTaskStatus.valueOf(rs.getString("status")));
		task.setVersion(rs.getInt("version"));
		task.setCreatedAt(rs.getLong("created_at"));
		task.setUpdatedAt(rs.getLong("updated_at"));
		return task;
	}

}
