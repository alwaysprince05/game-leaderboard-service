package dev.princemaurya.leaderboard.leaderboard;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dev.princemaurya.leaderboard.score.PlayerScore;

import org.springframework.data.redis.connection.RedisZSetCommands.ZAddArgs;
import org.springframework.data.redis.connection.zset.DefaultTuple;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardCache {

	private final StringRedisTemplate redis;

	private final ZSetOperations<String, String> zSet;

	public LeaderboardCache(StringRedisTemplate redis) {
		this.redis = redis;
		this.zSet = redis.opsForZSet();
	}

	public void setTotals(String gameId, Collection<PlayerScore> totals) {
		if (totals.isEmpty()) {
			return;
		}
		// absolute totals, not increments, so retries are harmless; totals never decrease, so
		// ZADD GT keeps a stale write (late retry, concurrent rebuild) from overwriting a newer one
		byte[] key = key(gameId).getBytes(StandardCharsets.UTF_8);
		Set<Tuple> tuples = totals.stream()
			.map(score -> (Tuple) new DefaultTuple(score.getId().getPlayerId().getBytes(StandardCharsets.UTF_8),
					(double) score.getTotalPoints()))
			.collect(Collectors.toSet());
		redis.execute((RedisCallback<Long>) connection -> connection.zSetCommands()
			.zAdd(key, tuples, ZAddArgs.empty().gt()));
	}

	public List<LeaderboardEntry> top(String gameId, int limit) {
		Set<TypedTuple<String>> tuples = zSet.reverseRangeWithScores(key(gameId), 0, limit - 1L);
		List<LeaderboardEntry> entries = new ArrayList<>();
		if (tuples == null) {
			return entries;
		}
		long rank = 1;
		for (TypedTuple<String> tuple : tuples) {
			entries.add(new LeaderboardEntry(rank++, tuple.getValue(), Math.round(tuple.getScore())));
		}
		return entries;
	}

	public Optional<LeaderboardEntry> player(String gameId, String playerId) {
		Long zeroBasedRank = zSet.reverseRank(key(gameId), playerId);
		Double score = zSet.score(key(gameId), playerId);
		if (zeroBasedRank == null || score == null) {
			return Optional.empty();
		}
		return Optional.of(new LeaderboardEntry(zeroBasedRank + 1, playerId, Math.round(score)));
	}

	private static String key(String gameId) {
		return "leaderboard:" + gameId;
	}

}
