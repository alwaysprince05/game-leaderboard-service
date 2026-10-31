package dev.princemaurya.leaderboard.score;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_matches")
public class ProcessedMatch {

	@Id
	@Column(name = "match_id")
	private UUID matchId;

	@Column(name = "game_id", nullable = false, length = 64)
	private String gameId;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	protected ProcessedMatch() {
	}

	public UUID getMatchId() {
		return matchId;
	}

	public String getGameId() {
		return gameId;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}

}
