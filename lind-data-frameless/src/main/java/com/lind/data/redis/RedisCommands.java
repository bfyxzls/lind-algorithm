package com.lind.data.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.Tuple;

/**
 * Redis 命令抽象，便于封装场景与单测替换实现。
 */
public interface RedisCommands extends AutoCloseable {

	String set(String key, String value);

	String set(String key, String value, SetParams params);

	String get(String key);

	long del(String... keys);

	boolean exists(String key);

	long expire(String key, long seconds);

	long incr(String key);

	long incrBy(String key, long increment);

	long decr(String key);

	boolean setBit(String key, long offset, boolean value);

	boolean getBit(String key, long offset);

	long bitCount(String key);

	long pfAdd(String key, String... elements);

	long pfCount(String key);

	long sAdd(String key, String... members);

	long sRem(String key, String... members);

	boolean sIsMember(String key, String member);

	Set<String> sMembers(String key);

	Set<String> sInter(String... keys);

	Set<String> sUnion(String... keys);

	Set<String> sDiff(String... keys);

	long zAdd(String key, double score, String member);

	long zRem(String key, String... members);

	Double zScore(String key, String member);

	long zCard(String key);

	long zRank(String key, String member);

	long zRevRank(String key, String member);

	List<String> zRange(String key, long start, long stop);

	List<String> zRevRange(String key, long start, long stop);

	List<Tuple> zRangeByScoreWithScores(String key, double min, double max);

	List<Tuple> zRevRangeWithScores(String key, long start, long stop);

	long zRemRangeByScore(String key, double min, double max);

	long lPush(String key, String... values);

	long rPush(String key, String... values);

	String lPop(String key);

	String rPop(String key);

	String bRPopLPush(String source, String destination, int timeoutSeconds);

	long lLen(String key);

	long publish(String channel, String message);

	void subscribe(RedisMessageListener listener, String... channels);

	long geoAdd(String key, double longitude, double latitude, String member);

	List<GeoRadiusResponse> geoRadius(String key, double longitude, double latitude, double radius, GeoUnit unit,
			GeoRadiusParam param);

	Object eval(String script, List<String> keys, List<String> args);

	@Override
	void close();

}
