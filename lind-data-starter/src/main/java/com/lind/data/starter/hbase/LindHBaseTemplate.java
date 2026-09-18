package com.lind.data.starter.hbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * 轻量 HBase 模板（put / get / scan / delete）。
 */
public class LindHBaseTemplate {

	private final Connection connection;

	public LindHBaseTemplate(Connection connection) {
		this.connection = Objects.requireNonNull(connection, "connection");
	}

	public Connection connection() {
		return connection;
	}

	public void put(String table, String rowKey, String family, String qualifier, byte[] value) throws IOException {
		try (Table t = connection.getTable(TableName.valueOf(table))) {
			Put put = new Put(Bytes.toBytes(rowKey));
			put.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), value);
			t.put(put);
		}
	}

	public void putString(String table, String rowKey, String family, String qualifier, String value)
			throws IOException {
		put(table, rowKey, family, qualifier, value.getBytes(StandardCharsets.UTF_8));
	}

	public Optional<byte[]> get(String table, String rowKey, String family, String qualifier) throws IOException {
		try (Table t = connection.getTable(TableName.valueOf(table))) {
			Get get = new Get(Bytes.toBytes(rowKey));
			get.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
			Result result = t.get(get);
			byte[] value = result.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier));
			return Optional.ofNullable(value);
		}
	}

	public Optional<String> getString(String table, String rowKey, String family, String qualifier) throws IOException {
		return get(table, rowKey, family, qualifier).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
	}

	public List<Result> scan(String table, String startRow, String stopRow) throws IOException {
		try (Table t = connection.getTable(TableName.valueOf(table))) {
			Scan scan = new Scan();
			if (startRow != null && !startRow.isEmpty()) {
				scan.withStartRow(Bytes.toBytes(startRow));
			}
			if (stopRow != null && !stopRow.isEmpty()) {
				scan.withStopRow(Bytes.toBytes(stopRow));
			}
			List<Result> rows = new ArrayList<>();
			try (ResultScanner scanner = t.getScanner(scan)) {
				for (Result r : scanner) {
					rows.add(r);
				}
			}
			return rows;
		}
	}

	public void delete(String table, String rowKey) throws IOException {
		try (Table t = connection.getTable(TableName.valueOf(table))) {
			t.delete(new Delete(Bytes.toBytes(rowKey)));
		}
	}

}
