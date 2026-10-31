package dev.princemaurya.leaderboard.score;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_scores")
public class PlayerScore {

	@EmbeddedId
	private PlayerScoreId id;

	@Column(name = "total_points", nullable = false)
	private long totalPoints;

	@Column(name = "matches_played", nullable = false)
	private int matchesPlayed;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PlayerScore() {
	}

	public PlayerScoreId getId() {
		return id;
	}

	public long getTotalPoints() {
		return totalPoints;
	}

	public int getMatchesPlayed() {
		return matchesPlayed;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
