package com.lind.data.starter.mongodb;

import com.lind.data.starter.LindDataProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@AutoConfiguration
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnProperty(prefix = "lind.data.mongo", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LindDataProperties.class)
public class MongoStarterAutoConfiguration {

	@Bean
	@ConditionalOnBean(MongoTemplate.class)
	@ConditionalOnMissingBean
	public LindSpringMongo lindSpringMongo(MongoTemplate mongoTemplate) {
		return new LindSpringMongo(mongoTemplate);
	}

}
