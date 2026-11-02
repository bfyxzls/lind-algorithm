package com.lind.stream.buffer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class CoalescingStringFluxTest {

	@Test
	void flushesWhenCharThresholdReached() {
		Flux<String> source = Flux.just("aa", "bb", "cc", "dd", "ee");
		StepVerifier.create(CoalescingStringFlux.coalesce(source, 4, Duration.ofSeconds(10))).expectNext("aabb")
				.expectNext("ccdd").expectNext("ee").verifyComplete();
	}

	@Test
	void flushesOnMaxDelayEvenIfBelowThreshold() {
		List<String> out = CoalescingStringFlux.coalesce(Flux.just("x", "y"), 100, Duration.ofMillis(50)).collectList()
				.block(Duration.ofSeconds(2));
		assertThat(out).containsExactly("xy");
	}

	@Test
	void shortMaxDelay_dominates_largeFlushChars() {
		// 对齐线上踩坑：token 慢、maxDelay 短时，flushChars 再大也攒不成大包
		Flux<String> source = Flux.just("aa", "bb", "cc", "dd").delayElements(Duration.ofMillis(40));
		List<String> out = CoalescingStringFlux.coalesce(source, 480, Duration.ofMillis(50)).collectList()
				.block(Duration.ofSeconds(5));
		assertThat(out).isNotEmpty();
		assertThat(out.stream().allMatch(s -> s.length() < 480)).isTrue();
		assertThat(String.join("", out)).isEqualTo("aabbccdd");
	}

	@Test
	void disabledPropertiesPassThrough() {
		SseCoalesceProperties props = new SseCoalesceProperties();
		props.setEnabled(false);
		List<String> out = CoalescingStringFlux.coalesce(Flux.just("a", "b", "c"), props).collectList()
				.block(Duration.ofSeconds(1));
		assertThat(out).containsExactly("a", "b", "c");
	}

	@Test
	void reducesEventCountVersusRawTokens() {
		List<String> tokens = List.of("你", "好", "，", "世", "界", "！", "这", "是", "测", "试");
		int raw = tokens.size();
		List<String> coalesced = CoalescingStringFlux.coalesce(Flux.fromIterable(tokens), 6, Duration.ofSeconds(5))
				.collectList().block(Duration.ofSeconds(1));
		assertThat(coalesced).isNotEmpty();
		assertThat(coalesced.size()).isLessThan(raw);
		assertThat(String.join("", coalesced)).isEqualTo(String.join("", tokens));
	}

}
