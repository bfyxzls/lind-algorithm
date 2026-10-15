package com.lind.data.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * lind-data-starter 配置前缀：{@code lind.data.*}
 */
@ConfigurationProperties(prefix = "lind.data")
public class LindDataProperties {

	private final Redis redis = new Redis();

	private final Redisson redisson = new Redisson();

	private final Mongo mongo = new Mongo();

	private final Elasticsearch elasticsearch = new Elasticsearch();

	private final Hbase hbase = new Hbase();

	private final Doris doris = new Doris();

	public Redis getRedis() {
		return redis;
	}

	public Redisson getRedisson() {
		return redisson;
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

	public Doris getDoris() {
		return doris;
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

	public static class Redisson {

		/** 是否启用 Redisson 场景 Bean（需 classpath 有 RedissonClient）。 */
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

	/**
	 * Apache Doris（MySQL 协议查询 + FE HTTP Stream Load）。默认关闭，避免误连。
	 */
	public static class Doris {

		private boolean enabled = false;

		/** JDBC 地址，默认连 FE query port 9030。 */
		private String jdbcUrl = "jdbc:mysql://127.0.0.1:9030/demo";

		private String username = "root";

		private String password = "";

		private String driverClassName = "com.mysql.cj.jdbc.Driver";

		/** FE HTTP 主机（Stream Load）。 */
		private String feHttpHost = "127.0.0.1";

		/** FE HTTP 端口，默认 8030。 */
		private int feHttpPort = 8030;

		/** Stream Load 默认库名。 */
		private String database = "demo";

		/** Stream Load 最大过滤比例。 */
		private double maxFilterRatio = 0.1;

		/** Stream Load 超时（秒）。 */
		private int timeoutSeconds = 600;

		/** Stream Load label 前缀。 */
		private String labelPrefix = "lind";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getJdbcUrl() {
			return jdbcUrl;
		}

		public void setJdbcUrl(String jdbcUrl) {
			this.jdbcUrl = jdbcUrl;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getDriverClassName() {
			return driverClassName;
		}

		public void setDriverClassName(String driverClassName) {
			this.driverClassName = driverClassName;
		}

		public String getFeHttpHost() {
			return feHttpHost;
		}

		public void setFeHttpHost(String feHttpHost) {
			this.feHttpHost = feHttpHost;
		}

		public int getFeHttpPort() {
			return feHttpPort;
		}

		public void setFeHttpPort(int feHttpPort) {
			this.feHttpPort = feHttpPort;
		}

		public String getDatabase() {
			return database;
		}

		public void setDatabase(String database) {
			this.database = database;
		}

		public double getMaxFilterRatio() {
			return maxFilterRatio;
		}

		public void setMaxFilterRatio(double maxFilterRatio) {
			this.maxFilterRatio = maxFilterRatio;
		}

		public int getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public void setTimeoutSeconds(int timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}

		public String getLabelPrefix() {
			return labelPrefix;
		}

		public void setLabelPrefix(String labelPrefix) {
			this.labelPrefix = labelPrefix;
		}

	}

}
