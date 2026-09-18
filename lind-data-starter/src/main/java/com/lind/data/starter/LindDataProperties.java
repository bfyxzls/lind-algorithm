package com.lind.data.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * lind-data-starter 配置前缀：{@code lind.data.*}
 */
@ConfigurationProperties(prefix = "lind.data")
public class LindDataProperties {

	private final Redis redis = new Redis();

	private final Mongo mongo = new Mongo();

	private final Elasticsearch elasticsearch = new Elasticsearch();

	private final Hbase hbase = new Hbase();

	public Redis getRedis() {
		return redis;
	}

	public Mongo getMongo() {
		return mongo;
	}

	public Elasticsearch getElasticsearch() {
		return elasticsearch;
	}

	public Hbase getHbase() {
		return hbase;
	}

	public static class Redis {

		/** 是否启用 Redis 场景 Bean（需 classpath 有 Spring Data Redis）。 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Mongo {

		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Elasticsearch {

		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Hbase {

		private boolean enabled = false;

		private String zookeeperQuorum = "127.0.0.1";

		private String zookeeperClientPort = "2181";

		private String zookeeperZnodeParent = "/hbase";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getZookeeperQuorum() {
			return zookeeperQuorum;
		}

		public void setZookeeperQuorum(String zookeeperQuorum) {
			this.zookeeperQuorum = zookeeperQuorum;
		}

		public String getZookeeperClientPort() {
			return zookeeperClientPort;
		}

		public void setZookeeperClientPort(String zookeeperClientPort) {
			this.zookeeperClientPort = zookeeperClientPort;
		}

		public String getZookeeperZnodeParent() {
			return zookeeperZnodeParent;
		}

		public void setZookeeperZnodeParent(String zookeeperZnodeParent) {
			this.zookeeperZnodeParent = zookeeperZnodeParent;
		}

	}

}
