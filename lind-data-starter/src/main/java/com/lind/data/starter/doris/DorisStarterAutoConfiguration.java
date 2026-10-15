package com.lind.data.starter.doris;

import com.lind.data.starter.LindDataProperties;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Apache Doris 自动配置：按 {@code lind.data.doris.*} 创建 DataSource / JdbcTemplate / Stream
 * Load，并装配 {@link LindSpringDoris}。
 * <p>
 * 默认关闭（{@code enabled=false}）。若业务已提供 {@link DataSource} / {@link JdbcTemplate}，则复用并不再创建。
 */
@AutoConfiguration
@ConditionalOnClass({ DataSource.class, JdbcTemplate.class })
@ConditionalOnProperty(prefix = "lind.data.doris", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LindDataProperties.class)
public class DorisStarterAutoConfiguration {

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(DataSource.class)
	public DataSource dorisDataSource(LindDataProperties properties) {
		LindDataProperties.Doris doris = properties.getDoris();
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setPoolName("lind-doris");
		dataSource.setJdbcUrl(doris.getJdbcUrl());
		dataSource.setUsername(doris.getUsername());
		dataSource.setPassword(doris.getPassword());
		dataSource.setDriverClassName(doris.getDriverClassName());
		dataSource.setMaximumPoolSize(10);
		dataSource.setMinimumIdle(1);
		return dataSource;
	}

	@Bean
	@ConditionalOnMissingBean
	public JdbcTemplate dorisJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	@ConditionalOnMissingBean
	public DorisStreamLoad dorisStreamLoad(LindDataProperties properties) {
		return new DorisStreamLoad(properties.getDoris());
	}

	@Bean
	@ConditionalOnMissingBean
	public LindSpringDoris lindSpringDoris(JdbcTemplate dorisJdbcTemplate, DorisStreamLoad dorisStreamLoad) {
		return new LindSpringDoris(dorisJdbcTemplate, dorisStreamLoad);
	}

}
