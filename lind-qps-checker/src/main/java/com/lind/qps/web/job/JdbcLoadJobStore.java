package com.lind.qps.web.job;

import com.lind.qps.engine.CapacityFinder;
import com.lind.qps.engine.LoadConfig;
import com.lind.qps.engine.LoadReport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * H2 / JDBC 压测任务存储。
 */
@Repository
public class JdbcLoadJobStore implements LoadJobStore {

	private final JdbcTemplate jdbc;

	private final LoadJobCodec codec;

	private final RowMapper<LoadJob> rowMapper = this::mapRow;

	public JdbcLoadJobStore(JdbcTemplate jdbc, LoadJobCodec codec) {
		this.jdbc = jdbc;
		this.codec = codec;
	}

	@Override
	public void insert(LoadJob job) {
		LoadConfig config = job.config();
		jdbc.update("""
				INSERT INTO qps_job (
				  id, mode, status, message, url, method, concurrency, duration_seconds, timeout_ms,
				  headers_json, query_params_json, form_params_json, body,
				  start_concurrency, step, max_concurrency, min_success_rate_percent, max_allowed_latency_ms,
				  created_at, finished_at, report_json, capacity_json
				) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
				""", job.id(), job.mode().name(), job.status().name(), job.message(), config.url().toString(),
				config.method(), config.concurrency(), config.duration().toSeconds(),
				config.requestTimeout().toMillis(), codec.writeMap(config.headers()),
				codec.writeMap(config.queryParams()), codec.writeMap(config.formParams()), config.body(),
				job.startConcurrency(), job.step(), job.maxConcurrency(), job.minSuccessRatePercent(),
				job.maxAllowedLatencyMillis(), Timestamp.from(job.createdAt()),
				job.finishedAt() == null ? null : Timestamp.from(job.finishedAt()), codec.writeReport(job.report()),
				codec.writeCapacity(job.capacityResult()));
	}

	@Override
	public void updateFinished(LoadJob job) {
		jdbc.update("""
				UPDATE qps_job SET
				  status = ?, message = ?, finished_at = ?, report_json = ?, capacity_json = ?
				WHERE id = ?
				""", job.status().name(), job.message(),
				job.finishedAt() == null ? null : Timestamp.from(job.finishedAt()), codec.writeReport(job.report()),
				codec.writeCapacity(job.capacityResult()), job.id());
	}

	@Override
	public Optional<LoadJob> findById(String id) {
		List<LoadJob> list = jdbc.query("SELECT * FROM qps_job WHERE id = ?", rowMapper, id);
		return list.stream().findFirst();
	}

	@Override
	public List<LoadJob> findRecent(int limit) {
		int safe = Math.max(1, Math.min(limit, 500));
		return jdbc.query("""
				SELECT * FROM qps_job
				ORDER BY created_at DESC
				LIMIT ?
				""", rowMapper, safe);
	}

	@Override
	public int markInterruptedRunning(String message) {
		return jdbc.update("""
				UPDATE qps_job
				SET status = ?, message = ?, finished_at = ?
				WHERE status = ?
				""", LoadJobStatus.FAILED.name(), message, Timestamp.from(Instant.now()), LoadJobStatus.RUNNING.name());
	}

	private LoadJob mapRow(ResultSet rs, int rowNum) throws SQLException {
		LoadConfig config = codec.toConfig(rs.getString("url"), rs.getString("method"), rs.getInt("concurrency"),
				rs.getLong("duration_seconds"), rs.getLong("timeout_ms"), rs.getString("headers_json"),
				rs.getString("query_params_json"), rs.getString("form_params_json"), rs.getString("body"));
		Instant createdAt = rs.getTimestamp("created_at").toInstant();
		LoadJob job = new LoadJob(rs.getString("id"), LoadMode.valueOf(rs.getString("mode")), config, createdAt);
		job.status(LoadJobStatus.valueOf(rs.getString("status")));
		job.message(rs.getString("message"));
		job.startConcurrency(rs.getInt("start_concurrency"));
		job.step(rs.getInt("step"));
		job.maxConcurrency(rs.getInt("max_concurrency"));
		job.minSuccessRatePercent(rs.getDouble("min_success_rate_percent"));
		job.maxAllowedLatencyMillis(rs.getLong("max_allowed_latency_ms"));
		Timestamp finished = rs.getTimestamp("finished_at");
		if (finished != null) {
			job.finishedAt(finished.toInstant());
		}
		LoadReport report = codec.readReport(rs.getString("report_json"));
		if (report != null) {
			job.report(report);
		}
		CapacityFinder.CapacityResult capacity = codec.readCapacity(rs.getString("capacity_json"));
		if (capacity != null) {
			job.capacityResult(capacity);
		}
		return job;
	}

}
