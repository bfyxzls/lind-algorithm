package com.lind.data.redis;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.Tuple;

/**
 * 基于 Jedis {@link JedisPooled} 的 {@link RedisCommands} 实现。
 */
public final class JedisRedisCommands implements RedisCommands {

	private final JedisPooled jedis;

	private final boolean closeOnClose;

	public JedisRedisCommands(JedisPooled jedis) {
		this(jedis, true);
	}

	public JedisRedisCommands(JedisPooled jedis, boolean closeOnClose) {
		this.jedis = Objects.requireNonNull(jedis, "jedis");
		this.closeOnClose = closeOnClose;
	}

	public static JedisRedisCommands connect(String host, int port) {
		return connect(host, port, null);
	}

	public static JedisRedisCommands connect(String host, int port, String password) {
		JedisClientConfig config = password == null || password.isBlank() ? DefaultJedisClientConfig.builder().build()
				: DefaultJedisClientConfig.builder().password(password).build();
		return new JedisRedisCommands(new JedisPooled(new HostAndPort(host, port), config));
	}

	public JedisPooled jedis() {
		return jedis;
	}

	@Override
	public String set(String key, String value) {
		return jedis.set(key, value);
	}

	@Override
	public String set(String key, String value, SetParams params) {
		return jedis.set(key, value, params);
	}

	@Override
	public String get(String key) {
		return jedis.get(key);
	}

	@Override
	public long del(String... keys) {
		return jedis.del(keys);
	}

	@Override
	public boolean exists(String key) {
		return jedis.exists(key);
	}

	@Override
	public long expire(String key, long seconds) {
		return jedis.expire(key, seconds);
	}

	@Override
	public long incr(String key) {
		return jedis.incr(key);
	}

	@Override
	public long incrBy(String key, long increment) {
		return jedis.incrBy(key, increment);
	}

	@Override
	public long decr(String key) {
		return jedis.decr(key);
	}

	@Override
	public boolean setBit(String key, long offset, boolean value) {
		return jedis.setbit(key, offset, value);
	}

	@Override
	public boolean getBit(String key, long offset) {
		return jedis.getbit(key, offset);
	}

	@Override
	public long bitCount(String key) {
		return jedis.bitcount(key);
	}

	@Override
	public long pfAdd(String key, String... elements) {
		return jedis.pfadd(key, elements);
	}

	@Override
	public long pfCount(String key) {
		return jedis.pfcount(key);
	}

	@Override
	public long sAdd(String key, String... members) {
		return jedis.sadd(key, members);
	}

	@Override
	public long sRem(String key, String... members) {
		return jedis.srem(key, members);
	}

	@Override
	public boolean sIsMember(String key, String member) {
		return jedis.sismember(key, member);
	}

	@Override
	public Set<String> sMembers(String key) {
		return jedis.smembers(key);
	}

	@Override
	public Set<String> sInter(String... keys) {
		return jedis.sinter(keys);
	}

	@Override
	public Set<String> sUnion(String... keys) {
		return jedis.sunion(keys);
	}

	@Override
	public Set<String> sDiff(String... keys) {
		return jedis.sdiff(keys);
	}

	@Override
	public long zAdd(String key, double score, String member) {
		return jedis.zadd(key, score, member);
	}

	@Override
	public long zRem(String key, String... members) {
		return jedis.zrem(key, members);
	}

	@Override
	public Double zScore(String key, String member) {
		return jedis.zscore(key, member);
	}

	@Override
	public long zCard(String key) {
		return jedis.zcard(key);
	}

	@Override
	public long zRank(String key, String member) {
		Long rank = jedis.zrank(key, member);
		return rank == null ? -1L : rank;
	}

	@Override
	public long zRevRank(String key, String member) {
		Long rank = jedis.zrevrank(key, member);
		return rank == null ? -1L : rank;
	}

	@Override
	public List<String> zRange(String key, long start, long stop) {
		return jedis.zrange(key, start, stop);
	}

	@Override
	public List<String> zRevRange(String key, long start, long stop) {
		return jedis.zrevrange(key, start, stop);
	}

	@Override
	public List<Tuple> zRangeByScoreWithScores(String key, double min, double max) {
		return jedis.zrangeByScoreWithScores(key, min, max);
	}

	@Override
	public List<Tuple> zRevRangeWithScores(String key, long start, long stop) {
		return jedis.zrevrangeWithScores(key, start, stop);
	}

	@Override
	public long zRemRangeByScore(String key, double min, double max) {
		return jedis.zremrangeByScore(key, min, max);
	}

	@Override
	public long lPush(String key, String... values) {
		return jedis.lpush(key, values);
	}

	@Override
	public long rPush(String key, String... values) {
		return jedis.rpush(key, values);
	}

	@Override
	public String lPop(String key) {
		return jedis.lpop(key);
	}

	@Override
	public String rPop(String key) {
		return jedis.rpop(key);
	}

	@Override
	public String bRPopLPush(String source, String destination, int timeoutSeconds) {
		return jedis.brpoplpush(source, destination, timeoutSeconds);
	}

	@Override
	public long lLen(String key) {
		return jedis.llen(key);
	}

	@Override
	public long publish(String channel, String message) {
		return jedis.publish(channel, message);
	}

	@Override
	public void subscribe(RedisMessageListener listener, String... channels) {
		Objects.requireNonNull(listener, "listener");
		jedis.subscribe(new JedisPubSub() {
			@Override
			public void onMessage(String channel, String message) {
				listener.onMessage(channel, message);
			}
		}, channels);
	}

	@Override
	public long geoAdd(String key, double longitude, double latitude, String member) {
		return jedis.geoadd(key, longitude, latitude, member);
	}

	@Override
	public List<GeoRadiusResponse> geoRadius(String key, double longitude, double latitude, double radius, GeoUnit unit,
			GeoRadiusParam param) {
		return jedis.georadius(key, longitude, latitude, radius, unit, param);
	}

	@Override
	public Object eval(String script, List<String> keys, List<String> args) {
		return jedis.eval(script, keys, args);
	}

	@Override
	public void close() {
		if (closeOnClose) {
			jedis.close();
		}
	}

}
