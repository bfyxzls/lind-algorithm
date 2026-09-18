package com.lind.data.starter.hbase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HBase 场景单测（mock {@link Connection}/{@link Table}，无需本机 HBase）。
 */
@ExtendWith(MockitoExtension.class)
class HBaseScenarioTest {

	@Mock
	private Connection connection;

	@Mock
	private Table table;

	private LindHBaseTemplate hbase;

	@BeforeEach
	void setUp() throws Exception {
		lenient().when(connection.getTable(any(TableName.class))).thenReturn(table);
		hbase = new LindHBaseTemplate(connection);
	}

	@Test
	void connectionAccessor() {
		assertThat(hbase.connection()).isSameAs(connection);
	}

	@Test
	void putStringAndGetString() throws Exception {
		byte[] family = Bytes.toBytes("cf");
		byte[] qualifier = Bytes.toBytes("name");
		byte[] value = "lind".getBytes(StandardCharsets.UTF_8);
		Result result = mock(Result.class);
		when(table.get(any(Get.class))).thenReturn(result);
		when(result.getValue(family, qualifier)).thenReturn(value);

		hbase.putString("user_profile", "u1001", "cf", "name", "lind");
		Optional<String> got = hbase.getString("user_profile", "u1001", "cf", "name");

		assertThat(got).contains("lind");
		verify(table).put(any(Put.class));
		verify(table).get(any(Get.class));
		verify(table, atLeastOnce()).close();
	}

	@Test
	void getMissingReturnsEmpty() throws Exception {
		Result result = mock(Result.class);
		when(table.get(any(Get.class))).thenReturn(result);
		when(result.getValue(any(byte[].class), any(byte[].class))).thenReturn(null);

		assertThat(hbase.get("user_profile", "missing", "cf", "name")).isEmpty();
	}

	@Test
	void scanRange() throws Exception {
		Result row = mock(Result.class);
		ResultScanner scanner = mock(ResultScanner.class);
		when(table.getScanner(any(Scan.class))).thenReturn(scanner);
		when(scanner.iterator()).thenReturn(List.of(row).iterator());

		List<Result> rows = hbase.scan("user_profile", "u1000", "u2000");
		assertThat(rows).containsExactly(row);

		ArgumentCaptor<Scan> scanCaptor = ArgumentCaptor.forClass(Scan.class);
		verify(table).getScanner(scanCaptor.capture());
		assertThat(scanCaptor.getValue().getStartRow()).isEqualTo(Bytes.toBytes("u1000"));
		assertThat(scanCaptor.getValue().getStopRow()).isEqualTo(Bytes.toBytes("u2000"));
	}

	@Test
	void deleteRow() throws Exception {
		hbase.delete("user_profile", "u1001");
		verify(table).delete(any(Delete.class));
		verify(table).close();
	}

}
