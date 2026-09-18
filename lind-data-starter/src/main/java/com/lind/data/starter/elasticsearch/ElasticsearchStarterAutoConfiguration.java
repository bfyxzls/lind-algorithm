package com.lind.data.starter.elasticsearch;

import com.lind.data.starter.LindDataProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@AutoConfiguration
@ConditionalOnClass(ElasticsearchOperations.class)
@ConditionalOnProperty(prefix = "lind.data.elasticsearch", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(LindDataProperties.class)
public class ElasticsearchStarterAutoConfiguration {

	@Bean
	@ConditionalOnBean(ElasticsearchOperations.class)
	@ConditionalOnMissingBean
	public LindSpringElasticsearch lindSpringElasticsearch(ElasticsearchOperations elasticsearchOperations) {
		return new LindSpringElasticsearch(elasticsearchOperations);
	}

}
