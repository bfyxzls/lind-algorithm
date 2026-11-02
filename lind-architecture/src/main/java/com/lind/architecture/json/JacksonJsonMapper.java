package com.lind.architecture.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 知识点 (3)：默认实现——Jackson。
 */
public final class JacksonJsonMapper implements JsonMapper {

	private final ObjectMapper mapper;

	public JacksonJsonMapper() {
		this(new ObjectMapper());
	}

	public JacksonJsonMapper(ObjectMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper");
	}

	@Override
	public void write(Object value, Writer writer) throws IOException {
		mapper.writeValue(writer, value);
	}

	@Override
	public <T> T read(Reader reader, Type type) throws IOException {
		return mapper.readValue(reader, mapper.constructType(type));
	}

}
