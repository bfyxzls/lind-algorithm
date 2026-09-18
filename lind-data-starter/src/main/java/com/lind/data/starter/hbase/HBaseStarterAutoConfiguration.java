package com.lind.data.starter.hbase;

import com.lind.data.starter.LindDataProperties;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(Connection.class)
@ConditionalOnProperty(prefix = "lind.data.hbase", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LindDataProperties.class)
public class HBaseStarterAutoConfiguration {

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	public Connection hbaseConnection(LindDataProperties properties) throws IOException {
		LindDataProperties.Hbase hbase = properties.getHbase();
		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", hbase.getZookeeperQuorum());
		conf.set("hbase.zookeeper.property.clientPort", hbase.getZookeeperClientPort());
		conf.set("zookeeper.znode.parent", hbase.getZookeeperZnodeParent());
		return ConnectionFactory.createConnection(conf);
	}

	@Bean
	@ConditionalOnMissingBean
	public LindHBaseTemplate lindHBaseTemplate(Connection hbaseConnection) {
		return new LindHBaseTemplate(hbaseConnection);
	}

}
