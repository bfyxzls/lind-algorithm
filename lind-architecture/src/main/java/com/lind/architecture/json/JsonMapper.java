package com.lind.architecture.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * 知识点 (3)：序列化抽象——协议层只依赖本接口，不绑死 Jackson。
 */
public interface JsonMapper {

	void write(Object value, Writer writer) throws IOException;

	default String writeToString(Object value) throws IOException {
		StringWriter sw = new StringWriter();
		write(value, sw);
		return sw.toString();
	}

	<T> T read(Reader reader, Type type) throws IOException;

	default <T> T read(String json, Type type) throws IOException {
		return read(new StringReader(json), type);
	}

}
