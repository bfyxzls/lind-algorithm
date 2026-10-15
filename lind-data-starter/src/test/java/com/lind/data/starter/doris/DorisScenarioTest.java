package com.lind.data.starter.doris;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lind.data.starter.LindDataProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Doris 场景单测（mock JdbcTemplate / HttpURLConnection，无需本机 Doris）。
 */
@ExtendWith(MockitoExtension.class)
class DorisScenarioTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	private LindDataProperties.Doris config;

	private DorisStreamLoad streamLoad;

	private LindSpringDoris doris;

	private final AtomicReference<HttpURLConnection> lastConnection = new AtomicReference<>();

	private final ByteArrayOutputStream requestBody = new ByteArrayOutputStream();

	@BeforeEach
	void setUp() throws Exception {
		config = new LindDataProperties.Doris();
		config.setFeHttpHost("fe.local");
		config.setFeHttpPort(8030);
		config.setDatabase("analytics");
		config.setUsername("root");
		config.setPassword("secret");
		config.setLabelPrefix("ut");
		config.setMaxFilterRatio(0.2);
		config.setTimeoutSeconds(60);
		streamLoad = new DorisStreamLoad(config, this::openConnection);
		doris = new LindSpringDoris(jdbcTemplate, streamLoad);
	}

	private HttpURLConnection openConnection(URL url) {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		lastConnection.set(connection);
		try {
			when(connection.getOutputStream()).thenReturn(requestBody);
		}
		catch (Exception ignored) {
			// mock setup
		}
		return connection;
	}

	@Test
	void facadeExposesJdbcAndStreamLoad() {
		assertThat(doris.jdbcTemplate()).isSameAs(jdbcTemplate);
		assertThat(doris.jdbc()).isNotNull();
		assertThat(doris.streamLoad()).isSameAs(streamLoad);
		assertThat(doris.streamLoad().config().getDatabase()).isEqualTo("analytics");
	}

	@Test
	void jdbcQueryForListAndUpdate() {
		when(jdbcTemplate.queryForList(eq("select id from t where c=?"), eq("a")))
				.thenReturn(List.of(Map.of("id", 1L)));
		when(jdbcTemplate.update(eq("insert into t(c) values(?)"), eq("b"))).thenReturn(1);

		DorisJdbcQuery jdbc = doris.jdbc();
		assertThat(jdbc.queryForList("select id from t where c=?", "a")).containsExactly(Map.of("id", 1L));
		assertThat(jdbc.update("insert into t(c) values(?)", "b")).isEqualTo(1);
		assertThat(jdbc.template()).isSameAs(jdbcTemplate);
	}

	@Test
	@SuppressWarnings("unchecked")
	void jdbcQueryWithRowMapperAndBatch() {
		RowMapper<String> mapper = (rs, i) -> rs.getString(1);
		when(jdbcTemplate.query(eq("select name from u"), any(RowMapper.class))).thenReturn(List.of("lind"));
		when(jdbcTemplate.queryForObject(eq("select count(*) from u"), eq(Long.class))).thenReturn(3L);
		List<Object[]> batch = List.<Object[]>of(new Object[] { "a" });
		when(jdbcTemplate.batchUpdate(eq("insert into u(name) values(?)"), eq(batch))).thenReturn(new int[] { 1 });

		DorisJdbcQuery jdbc = doris.jdbc();
		assertThat(jdbc.query("select name from u", mapper)).containsExactly("lind");
		assertThat(jdbc.queryForObject("select count(*) from u", Long.class)).isEqualTo(3L);
		assertThat(jdbc.batchUpdate("insert into u(name) values(?)", batch)).containsExactly(1);
	}

	@Test
	void streamLoadJsonSuccess() throws Exception {
		HttpURLConnection connection = prepareResponse(200,
				"{\"TxnId\":99,\"Label\":\"ut-1\",\"Status\":\"Success\",\"Message\":\"OK\"}");

		DorisStreamLoadResult result = doris.streamLoad().loadJson("orders", "[{\"id\":1}]");
		assertThat(result.success()).isTrue();
		assertThat(result.status()).isEqualTo("Success");
		assertThat(result.txnId()).isEqualTo(99L);
		assertThat(result.label()).isEqualTo("ut-1");
		assertThat(requestBody.toString(StandardCharsets.UTF_8)).isEqualTo("[{\"id\":1}]");

		verify(connection).setRequestMethod("PUT");
		verify(connection).setRequestProperty(eq("format"), eq("json"));
		verify(connection).setRequestProperty(eq("strip_outer_array"), eq("true"));
		verify(connection).setRequestProperty(eq("Expect"), eq("100-continue"));
		verify(connection).setRequestProperty(eq("Authorization"), any());
	}

	@Test
	void streamLoadCsvUsesExplicitLabel() throws Exception {
		HttpURLConnection connection = prepareResponse(200, "{\"Status\":\"Success\",\"Label\":\"manual-label\"}");

		DorisStreamLoadResult result = streamLoad.load("events", "1,a\n2,b".getBytes(StandardCharsets.UTF_8),
				DorisStreamLoad.FORMAT_CSV, "manual-label");
		assertThat(result.success()).isTrue();
		verify(connection).setRequestProperty(eq("label"), eq("manual-label"));
		verify(connection).setRequestProperty(eq("format"), eq("csv"));
	}

	@Test
	void streamLoadRejectsBlankTable() {
		assertThatThrownBy(() -> streamLoad.loadCsv(" ", "1,a")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void streamLoadResultParsesFailure() {
		DorisStreamLoadResult result = new DorisStreamLoadResult(200,
				"{\"Status\":\"Fail\",\"Message\":\"too many filtered rows\",\"Label\":\"x\",\"TxnId\":1}");
		assertThat(result.success()).isFalse();
		assertThat(result.message()).contains("filtered");
		assertThat(result.toString()).contains("Fail");
	}

	@Test
	void streamLoadResultTreatsPublishTimeoutAsSuccess() {
		assertThat(new DorisStreamLoadResult(200, "{\"Status\":\"Publish Timeout\"}").success()).isTrue();
	}

	private HttpURLConnection prepareResponse(int status, String body) throws Exception {
		requestBody.reset();
		HttpURLConnection connection = lastConnection.get();
		// connection is created lazily on load(); stub after first open via factory —
		// factory already set mock; reconfigure response on the mock that will be
		// returned
		HttpURLConnection prepared = mock(HttpURLConnection.class);
		when(prepared.getOutputStream()).thenReturn(requestBody);
		when(prepared.getResponseCode()).thenReturn(status);
		when(prepared.getInputStream()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		streamLoad = new DorisStreamLoad(config, url -> prepared);
		doris = new LindSpringDoris(jdbcTemplate, streamLoad);
		return prepared;
	}

}
