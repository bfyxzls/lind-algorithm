package com.lind.architecture.io;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StreamFamiliesDemoTest {

	@Test
	void outputStream_writesUtf8Bytes() throws Exception {
		assertThat(StreamFamiliesDemo.viaOutputStream())
				.isEqualTo(StreamFamiliesDemo.PAYLOAD.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void writer_keepsCharactersInMemory() throws Exception {
		assertThat(StreamFamiliesDemo.viaWriter()).isEqualTo(StreamFamiliesDemo.PAYLOAD);
	}

	@Test
	void charset_bridge_writer_to_outputStream() throws Exception {
		assertThat(StreamFamiliesDemo.viaWriterBridgeToOutputStream())
				.isEqualTo(StreamFamiliesDemo.PAYLOAD.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void probeWriter_on_bridge_sees_downstream_write() throws Exception {
		var probed = StreamFamiliesDemo.viaWriterBridgeWithProbe();
		assertThat(probed.utf8()).isEqualTo(StreamFamiliesDemo.PAYLOAD.getBytes(StandardCharsets.UTF_8));
		assertThat(probed.probeWriteCalls()).isGreaterThanOrEqualTo(1);
	}

	@Test
	void reader_bridge_roundTrips_same_payload() throws Exception {
		assertThat(StreamFamiliesDemo.roundTripThroughCharsetBridge()).isEqualTo(StreamFamiliesDemo.PAYLOAD);
	}

}
