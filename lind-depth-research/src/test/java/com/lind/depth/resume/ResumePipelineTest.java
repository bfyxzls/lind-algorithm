package com.lind.depth.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 场景三对照实验：断线后续传 vs 整段重生成。
 *
 * <p>
 * 读前建议看 {@code docs/工程化分析.md} 场景三，以及 {@link StreamingGenerator} / {@link ResumeService} /
 * {@link ClientEventDeduper}。
 */
class ResumePipelineTest {

	@TempDir
	Path tempDir;

	/**
	 * 完整无感恢复故事：
	 * <ol>
	 * <li>服务端生成 10 个 token，客户端收到前 6 个后「断线」</li>
	 * <li>重连带 Last-Event-ID=6，服务端只续推 7..10</li>
	 * <li>若续推与断线处有重叠，客户端按 id 去重</li>
	 * <li>玩具路径会把 1..10 全部再发一遍 → 重复</li>
	 * </ol>
	 */
	@Test
	@DisplayName("Last-Event-ID 续推 + 客户端去重 = 无感恢复；玩具重生成会重复")
	void resumeIsSeamlessWhileToyRegenerateDuplicates() throws Exception {
		String sessionId = "chat-1";
		FileWal wal = new FileWal(tempDir.resolve("wal"));
		InMemoryEventStream stream = new InMemoryEventStream();
		StreamingGenerator generator = new StreamingGenerator(sessionId, wal, stream);

		List<String> tokens = List.of("春", "眠", "不", "觉", "晓", "处", "处", "闻", "啼", "鸟");
		List<StreamEvent> clientInbox = new ArrayList<>();
		ClientEventDeduper deduper = new ClientEventDeduper();

		// ---------- 1. 首次推送：客户端只「成功接收」前 6 条后断线 ----------
		List<StreamEvent> all = generator.generate(tokens, event -> {
			if (event.id() <= 6) {
				if (deduper.accept(event)) {
					clientInbox.add(event);
				}
			}
			// id>6 假装网络已断，客户端没收到（但服务端 Stream/WAL 已有）
		});
		assertThat(all).hasSize(10);
		assertThat(wal.lastEventId(sessionId)).contains(10L);
		assertThat(clientInbox).hasSize(6);
		long lastEventId = clientInbox.get(clientInbox.size() - 1).id();
		assertThat(lastEventId).isEqualTo(6);

		// ---------- 2. 工程续推：只拿 id>6 的事件 ----------
		ResumeService resume = new ResumeService(wal, stream);
		List<StreamEvent> rest = resume.resumeAfter(sessionId, lastEventId);
		assertThat(rest).extracting(StreamEvent::id).containsExactly(7L, 8L, 9L, 10L);

		// 模拟续推时「7」重复推了一次（至少一次投递）
		List<StreamEvent> withDup = new ArrayList<>();
		withDup.add(rest.get(0));
		withDup.addAll(rest);
		for (StreamEvent e : withDup) {
			if (deduper.accept(e)) {
				clientInbox.add(e);
			}
		}
		// 去重后应正好 10 条，内容完整且无重复 id
		assertThat(clientInbox).hasSize(10);
		assertThat(clientInbox).extracting(StreamEvent::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
		assertThat(clientInbox).extracting(StreamEvent::data).containsExactlyElementsOf(tokens);

		// ---------- 3. 玩具：断线后整段重生成 → 客户端若不去重会翻倍 ----------
		List<String> toyAgain = ToyRegenerateFromScratch.regenerateAll(tokens);
		assertThat(toyAgain).hasSize(10);
		assertThat(toyAgain).isEqualTo(tokens); // 从头再来，用户会看到重复段落
	}

	@Test
	@DisplayName("伪造过大的 Last-Event-ID 应被 WAL 对账拒绝")
	void rejectForgedLastEventId() throws Exception {
		FileWal wal = new FileWal(tempDir.resolve("wal2"));
		InMemoryEventStream stream = new InMemoryEventStream();
		StreamingGenerator gen = new StreamingGenerator("s", wal, stream);
		gen.generate(List.of("a", "b"), null);

		ResumeService resume = new ResumeService(wal, stream);
		assertThatThrownBy(() -> resume.resumeAfter("s", 99)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("超过服务端 WAL");
	}

}
