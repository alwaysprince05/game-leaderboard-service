package dev.princemaurya.leaderboard.leaderboard;

import java.util.List;

import dev.princemaurya.leaderboard.score.PlayerScore;
import dev.princemaurya.leaderboard.score.PlayerScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * Reloads a game's Redis leaderboard from Postgres, the source of truth. Use it after Redis loses data
 * (restart without persistence, eviction, manual flush). Safe to run while matches are being processed:
 * cache writes use ZADD GT, so this can only raise a player's cached total to the committed value.
 */
@Service
public class LeaderboardRebuildService {

	static final int BATCH_SIZE = 1_000;

	private static final Logger log = LoggerFactory.getLogger(LeaderboardRebuildService.class);

	private final PlayerScoreRepository playerScores;

	private final LeaderboardCache leaderboardCache;

	public LeaderboardRebuildService(PlayerScoreRepository playerScores, LeaderboardCache leaderboardCache) {
		this.playerScores = playerScores;
		this.leaderboardCache = leaderboardCache;
	}

	/** Returns the number of players written to the cache. */
	public long rebuild(String gameId) {
		long loaded = 0;
		String after = "";
		List<PlayerScore> page;
		do {
			page = playerScores.findPageAfter(gameId, after, Limit.of(BATCH_SIZE));
			if (page.isEmpty()) {
				break;
			}
			leaderboardCache.setTotals(gameId, page);
			loaded += page.size();
			after = page.get(page.size() - 1).getId().getPlayerId();
		}
		while (page.size() == BATCH_SIZE);
		log.info("Rebuilt leaderboard for game {} with {} players", gameId, loaded);
		return loaded;
	}

}
