package dev.princemaurya.leaderboard.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchCompletedEvent(UUID matchId, String gameId, List<PlayerPoints> results, Instant completedAt) {

	public void validate() {
		if (matchId == null || gameId == null || gameId.isBlank() || results == null || results.isEmpty()) {
			throw new InvalidEventException("Event is missing matchId, gameId or results");
		}
		for (PlayerPoints result : results) {
			if (result == null || result.playerId() == null || result.playerId().isBlank() || result.points() < 0) {
				throw new InvalidEventException("Event " + matchId + " has an invalid player result");
			}
		}
	}

}
