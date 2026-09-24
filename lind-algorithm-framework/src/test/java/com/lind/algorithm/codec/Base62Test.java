package com.lind.algorithm.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * {@link Base62} 单元测试。
 */
public class Base62Test {

	@Test
	void longRoundTrip() {
		assertEquals("0", Base62.encode(0));
		assertEquals(0, Base62.decode("0"));
		assertEquals("1", Base62.encode(1));
		long value = 1_234_567_890_123_456_789L;
		assertEquals(value, Base62.decode(Base62.encode(value)));
	}

	@Test
	void bytesRoundTrip() {
		byte[] data = "hello-world".getBytes();
		assertArrayEquals(data, Base62.decodeToBytes(Base62.encode(data)));
		assertArrayEquals(new byte[] { 0, 1, 2 }, Base62.decodeToBytes(Base62.encode(new byte[] { 0, 1, 2 })));
	}

	@Test
	void rejectsNegativeAndBadChars() {
		assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1));
		assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc!"));
	}

}
