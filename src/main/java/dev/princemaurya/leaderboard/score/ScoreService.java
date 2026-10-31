package dev.princemaurya.leaderboard.score;

import java.util.List;

import dev.princemaurya.leaderboard.event.MatchCompletedEvent;
import dev.princemaurya.leaderboard.event.PlayerPoints;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {

	private final ProcessedMatchRepository processedMatches;

	private final PlayerScoreRepository playerScores;

	public ScoreService(ProcessedMatchRepository processedMatches, PlayerScoreRepository playerScores) {
		this.processedMatches = processedMatches;
		this.playerScores = playerScores;
	}

	@Transactional
	public ApplyResult apply(MatchCompletedEvent event) {
		boolean firstTime = processedMatches.markProcessed(event.matchId(), event.gameId()) == 1;
		if (firstTime) {
			for (PlayerPoints result : event.results()) {
				playerScores.addPoints(event.gameId(), result.playerId(), result.points());
			}
		}
		List<String> playerIds = event.results().stream().map(PlayerPoints::playerId).toList();
		return new ApplyResult(!firstTime, playerScores.findByIdGameIdAndIdPlayerIdIn(event.gameId(), playerIds));
	}

	public record ApplyResult(boolean duplicate, List<PlayerScore> totals) {
	}

}
