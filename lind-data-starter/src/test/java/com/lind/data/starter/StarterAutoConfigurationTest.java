package com.lind.data.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lind.data.starter.doris.DorisStarterAutoConfiguration;
import com.lind.data.starter.doris.DorisStreamLoad;
import com.lind.data.starter.doris.LindSpringDoris;
import com.lind.data.starter.elasticsearch.ElasticsearchStarterAutoConfiguration;
import com.lind.data.starter.elasticsearch.LindSpringElasticsearch;
import com.lind.data.starter.hbase.HBaseStarterAutoConfiguration;
import com.lind.data.starter.hbase.LindHBaseTemplate;
import com.lind.data.starter.redis.LindSpringRedis;
import com.lind.data.starter.redis.RedisStarterAutoConfiguration;
import com.lind.data.starter.redis.redisson.LindRedisson;
import com.lind.data.starter.redis.redisson.RedissonStarterAutoConfiguration;
import javax.sql.DataSource;
import org.apache.hadoop.hbase.client.Connection;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 各中间件 AutoConfiguration 开关与 Bean 装配。
 */
class StarterAutoConfigurationTest {

	@Test
	void redisRegistersAndCanDisable() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(RedisStarterAutoConfiguration.class))
				.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

		runner.run(ctx -> assertThat(ctx).hasSingleBean(LindSpringRedis.class));
		runner.withPropertyValues("lind.data.redis.enabled=false")
				.run(ctx -> assertThat(ctx).doesNotHaveBean(LindSpringRedis.class));
	}

	@Test
	void redissonRegistersAndCanDisable() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(RedissonStarterAutoConfiguration.class))
				.withBean(RedissonClient.class, () -> mock(RedissonClient.class));

		runner.run(ctx -> assertThat(ctx).hasSingleBean(LindRedisson.class));
		runner.withPropertyValues("lind.data.redisson.enabled=false")
				.run(ctx -> assertThat(ctx).doesNotHaveBean(LindRedisson.class));
	}

	@Test
	void elasticsearchRegistersAndCanDisable() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ElasticsearchStarterAutoConfiguration.class))
				.withBean(ElasticsearchOperations.class, () -> mock(ElasticsearchOperations.class));

		runner.run(ctx -> assertThat(ctx).hasSingleBean(LindSpringElasticsearch.class));
		runner.withPropertyValues("lind.data.elasticsearch.enabled=false")
				.run(ctx -> assertThat(ctx).doesNotHaveBean(LindSpringElasticsearch.class));
	}

	@Test
	void hbaseDisabledByDefaultAndRegistersWhenEnabledWithConnection() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(HBaseStarterAutoConfiguration.class));

		runner.run(ctx -> {
			assertThat(ctx).doesNotHaveBean(LindHBaseTemplate.class);
			assertThat(ctx).doesNotHaveBean(Connection.class);
		});

		runner.withPropertyValues("lind.data.hbase.enabled=true")
				.withBean(Connection.class, () -> mock(Connection.class))
				.run(ctx -> assertThat(ctx).hasSingleBean(LindHBaseTemplate.class));
	}

	@Test
	void dorisDisabledByDefaultAndRegistersWhenEnabledWithJdbc() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DorisStarterAutoConfiguration.class));

		runner.run(ctx -> {
			assertThat(ctx).doesNotHaveBean(LindSpringDoris.class);
			assertThat(ctx).doesNotHaveBean(DorisStreamLoad.class);
		});

		runner.withPropertyValues("lind.data.doris.enabled=true", "lind.data.doris.database=demo",
				"lind.data.doris.fe-http-host=127.0.0.1", "lind.data.doris.fe-http-port=8030")
				.withBean(DataSource.class, () -> mock(DataSource.class))
				.withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class)).run(ctx -> {
					assertThat(ctx).hasSingleBean(LindSpringDoris.class);
					assertThat(ctx).hasSingleBean(DorisStreamLoad.class);
				});
	}

}
