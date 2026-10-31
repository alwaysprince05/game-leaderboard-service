package dev.princemaurya.leaderboard.api;

public class PlayerNotRankedException extends RuntimeException {

	public PlayerNotRankedException(String gameId, String playerId) {
		super("Player '" + playerId + "' has no score in game '" + gameId + "'");
	}

}
