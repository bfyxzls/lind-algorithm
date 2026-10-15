package com.lind.qps.web.catalog;

import com.lind.qps.web.job.LoadMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCaseStore {

	private final JdbcTemplate jdbc;

	private final RowMapper<TestCase> rowMapper = JdbcCaseStore::mapRow;

	public JdbcCaseStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void insert(TestCase testCase) {
		jdbc.update("""
				INSERT INTO qps_case (
				  id, folder_id, name, description, url, method, mode, concurrency, duration_seconds, timeout_ms,
				  headers_text, query_params_text, form_params_text, body,
				  start_concurrency, step, max_concurrency, min_success_rate_percent, max_allowed_latency_ms,
				  created_at, updated_at
				) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
				""", testCase.id(), testCase.folderId(), testCase.name(), testCase.description(), testCase.url(),
				testCase.method(), testCase.mode().name(), testCase.concurrency(), testCase.durationSeconds(),
				testCase.timeoutMs(), testCase.headersText(), testCase.queryParamsText(), testCase.formParamsText(),
				testCase.body(), testCase.startConcurrency(), testCase.step(), testCase.maxConcurrency(),
				testCase.minSuccessRatePercent(), testCase.maxAllowedLatencyMillis(),
				Timestamp.from(testCase.createdAt()), Timestamp.from(testCase.updatedAt()));
	}

	public void update(TestCase testCase) {
		testCase.touch();
		jdbc.update("""
				UPDATE qps_case SET
				  folder_id = ?, name = ?, description = ?, url = ?, method = ?, mode = ?,
				  concurrency = ?, duration_seconds = ?, timeout_ms = ?,
				  headers_text = ?, query_params_text = ?, form_params_text = ?, body = ?,
				  start_concurrency = ?, step = ?, max_concurrency = ?,
				  min_success_rate_percent = ?, max_allowed_latency_ms = ?, updated_at = ?
				WHERE id = ?
				""", testCase.folderId(), testCase.name(), testCase.description(), testCase.url(), testCase.method(),
				testCase.mode().name(), testCase.concurrency(), testCase.durationSeconds(), testCase.timeoutMs(),
				testCase.headersText(), testCase.queryParamsText(), testCase.formParamsText(), testCase.body(),
				testCase.startConcurrency(), testCase.step(), testCase.maxConcurrency(),
				testCase.minSuccessRatePercent(), testCase.maxAllowedLatencyMillis(),
				Timestamp.from(testCase.updatedAt()), testCase.id());
	}

	public Optional<TestCase> findById(String id) {
		List<TestCase> list = jdbc.query("SELECT * FROM qps_case WHERE id = ?", rowMapper, id);
		return list.stream().findFirst();
	}

	public List<TestCase> findByFolder(String folderId) {
		return jdbc.query("""
				SELECT * FROM qps_case WHERE folder_id = ?
				ORDER BY updated_at DESC
				""", rowMapper, folderId);
	}

	public List<TestCase> findAll() {
		return jdbc.query("SELECT * FROM qps_case ORDER BY updated_at DESC", rowMapper);
	}

	public int countByFolder(String folderId) {
		Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM qps_case WHERE folder_id = ?", Integer.class, folderId);
		return n == null ? 0 : n;
	}

	public void delete(String id) {
		jdbc.update("DELETE FROM qps_case WHERE id = ?", id);
	}

	public static String newId() {
		return "c" + UUID.randomUUID().toString().replace("-", "").substring(0, 11);
	}

	private static TestCase mapRow(ResultSet rs, int rowNum) throws SQLException {
		TestCase testCase = new TestCase(rs.getString("id"));
		testCase.folderId(rs.getString("folder_id"));
		testCase.name(rs.getString("name"));
		testCase.description(rs.getString("description"));
		testCase.url(rs.getString("url"));
		testCase.method(rs.getString("method"));
		testCase.mode(LoadMode.valueOf(rs.getString("mode")));
		testCase.concurrency(rs.getInt("concurrency"));
		testCase.durationSeconds(rs.getLong("duration_seconds"));
		testCase.timeoutMs(rs.getLong("timeout_ms"));
		testCase.headersText(rs.getString("headers_text"));
		testCase.queryParamsText(rs.getString("query_params_text"));
		testCase.formParamsText(rs.getString("form_params_text"));
		testCase.body(rs.getString("body"));
		testCase.startConcurrency(rs.getInt("start_concurrency"));
		testCase.step(rs.getInt("step"));
		testCase.maxConcurrency(rs.getInt("max_concurrency"));
		testCase.minSuccessRatePercent(rs.getDouble("min_success_rate_percent"));
		testCase.maxAllowedLatencyMillis(rs.getLong("max_allowed_latency_ms"));
		Timestamp created = rs.getTimestamp("created_at");
		Timestamp updated = rs.getTimestamp("updated_at");
		if (created != null) {
			testCase.createdAt(created.toInstant());
		}
		if (updated != null) {
			testCase.updatedAt(updated.toInstant());
		}
		return testCase;
	}

}
