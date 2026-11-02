/**
 * 对照 {@code docs/elasticsearch-java的学习.md} §3，落地迷你 ToyDoc SDK。
 *
 * <p>
 * 分层：API（命名空间 Client + Fluent DSL）→ Transport（Endpoint 管道）→ JsonMapper（可替换序列化）。
 */
package com.lind.architecture;
