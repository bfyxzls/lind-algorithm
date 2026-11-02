package com.lind.depth.mmap;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;

/**
 * 显式释放 {@link MappedByteBuffer} 的文件映射。
 *
 * <p>
 * Windows 上若不 unmap，文件句柄会一直被占用，JUnit {@code @TempDir} 无法删除临时文件。 JDK 未提供稳定公开 API，这里通过
 * {@code Unsafe.invokeCleaner} 反射调用（仅教学 / 测试环境使用）。
 */
final class MmapUnmapper {

	private MmapUnmapper() {
	}

	static void unmap(MappedByteBuffer buffer) {
		if (buffer == null) {
			return;
		}
		try {
			Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			var field = unsafeClass.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			Object unsafe = field.get(null);
			Method invokeCleaner = unsafeClass.getMethod("invokeCleaner", java.nio.ByteBuffer.class);
			invokeCleaner.invoke(unsafe, buffer);
		}
		catch (ReflectiveOperationException ignored) {
			System.gc();
		}
	}

}
