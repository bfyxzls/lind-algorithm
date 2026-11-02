/**
 * 工程级后端深度研究模块。
 *
 * <p>
 * 设计对照文档：仓库根目录 {@code docs/工程化分析.md}。 目标不是再写一套业务框架，而是把文档里的「原理 + 工程兜底」落成可运行、可对比的代码。
 *
 * <ul>
 * <li>{@link com.lind.depth.stream} —— 场景一：流式攒包写出（DirectBuffer + 阈值 / 时间窗）</li>
 * <li>{@link com.lind.depth.mmap} —— 场景二：mmap + IVF/PQ 量化检索</li>
 * <li>{@link com.lind.depth.resume} —— 场景三：WAL + Stream 断点续传 + 幂等去重</li>
 * </ul>
 */
package com.lind.depth;
