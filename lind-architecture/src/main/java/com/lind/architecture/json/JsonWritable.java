package com.lind.architecture.json;

/**
 * 协议模型可输出为「JSON 树」（Map/List/标量），供 {@link SimpleJsonMapper} 等非 Jackson 实现使用。
 */
public interface JsonWritable {

	Object toJsonTree();

}
