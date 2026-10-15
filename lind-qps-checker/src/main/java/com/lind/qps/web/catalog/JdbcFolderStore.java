package com.lind.qps.web.catalog;

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
public class JdbcFolderStore {

	private final JdbcTemplate jdbc;

	private final RowMapper<TestFolder> rowMapper = JdbcFolderStore::mapRow;

	public JdbcFolderStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void insert(TestFolder folder) {
		jdbc.update("""
				INSERT INTO qps_folder (id, name, parent_id, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?)
				""", folder.id(), folder.name(), folder.parentId(), Timestamp.from(folder.createdAt()),
				Timestamp.from(folder.updatedAt()));
	}

	public void update(TestFolder folder) {
		folder.touch();
		jdbc.update("""
				UPDATE qps_folder SET name = ?, parent_id = ?, updated_at = ?
				WHERE id = ?
				""", folder.name(), folder.parentId(), Timestamp.from(folder.updatedAt()), folder.id());
	}

	public Optional<TestFolder> findById(String id) {
		List<TestFolder> list = jdbc.query("SELECT * FROM qps_folder WHERE id = ?", rowMapper, id);
		return list.stream().findFirst();
	}

	public List<TestFolder> findAll() {
		return jdbc.query("SELECT * FROM qps_folder ORDER BY name ASC", rowMapper);
	}

	public List<TestFolder> findByParent(String parentId) {
		if (parentId == null || parentId.isBlank()) {
			return jdbc.query("SELECT * FROM qps_folder WHERE parent_id IS NULL ORDER BY name ASC", rowMapper);
		}
		return jdbc.query("SELECT * FROM qps_folder WHERE parent_id = ? ORDER BY name ASC", rowMapper, parentId);
	}

	public int countChildren(String folderId) {
		Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM qps_folder WHERE parent_id = ?", Integer.class, folderId);
		return n == null ? 0 : n;
	}

	public void delete(String id) {
		jdbc.update("DELETE FROM qps_folder WHERE id = ?", id);
	}

	public long countAll() {
		Long n = jdbc.queryForObject("SELECT COUNT(*) FROM qps_folder", Long.class);
		return n == null ? 0L : n;
	}

	public static String newId() {
		return "f" + UUID.randomUUID().toString().replace("-", "").substring(0, 11);
	}

	private static TestFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp created = rs.getTimestamp("created_at");
		Timestamp updated = rs.getTimestamp("updated_at");
		return new TestFolder(rs.getString("id"), rs.getString("name"), rs.getString("parent_id"),
				created == null ? Instant.now() : created.toInstant(),
				updated == null ? Instant.now() : updated.toInstant());
	}

}
