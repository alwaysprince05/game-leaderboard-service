package dev.princemaurya.leaderboard.score;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PlayerScoreId implements Serializable {

	@Column(name = "game_id", nullable = false, length = 64)
	private String gameId;

	@Column(name = "player_id", nullable = false, length = 64)
	private String playerId;

	protected PlayerScoreId() {
	}

	public PlayerScoreId(String gameId, String playerId) {
		this.gameId = gameId;
		this.playerId = playerId;
	}

	public String getGameId() {
		return gameId;
	}

	public String getPlayerId() {
		return playerId;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PlayerScoreId that && Objects.equals(gameId, that.gameId)
				&& Objects.equals(playerId, that.playerId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gameId, playerId);
	}

}
