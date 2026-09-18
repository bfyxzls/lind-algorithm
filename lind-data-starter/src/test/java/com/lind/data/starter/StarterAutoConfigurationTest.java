package com.lind.data.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lind.data.starter.elasticsearch.ElasticsearchStarterAutoConfiguration;
import com.lind.data.starter.elasticsearch.LindSpringElasticsearch;
import com.lind.data.starter.hbase.HBaseStarterAutoConfiguration;
import com.lind.data.starter.hbase.LindHBaseTemplate;
import com.lind.data.starter.mongodb.LindSpringMongo;
import com.lind.data.starter.mongodb.MongoStarterAutoConfiguration;
import com.lind.data.starter.redis.LindSpringRedis;
import com.lind.data.starter.redis.RedisStarterAutoConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

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
	void mongoRegistersAndCanDisable() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MongoStarterAutoConfiguration.class))
				.withBean(MongoTemplate.class, () -> mock(MongoTemplate.class));

		runner.run(ctx -> assertThat(ctx).hasSingleBean(LindSpringMongo.class));
		runner.withPropertyValues("lind.data.mongo.enabled=false")
				.run(ctx -> assertThat(ctx).doesNotHaveBean(LindSpringMongo.class));
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

}
