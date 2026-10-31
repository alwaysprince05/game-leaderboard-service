package dev.princemaurya.leaderboard.api;

import java.util.List;

import dev.princemaurya.leaderboard.leaderboard.LeaderboardCache;
import dev.princemaurya.leaderboard.leaderboard.LeaderboardEntry;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games/{gameId}/leaderboard")
public class LeaderboardController {

	private final LeaderboardCache leaderboardCache;

	public LeaderboardController(LeaderboardCache leaderboardCache) {
		this.leaderboardCache = leaderboardCache;
	}

	@GetMapping
	public LeaderboardResponse top(@PathVariable @Pattern(regexp = Ids.PATTERN, message = Ids.MESSAGE) String gameId,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return new LeaderboardResponse(gameId, leaderboardCache.top(gameId, limit));
	}

	@GetMapping("/players/{playerId}")
	public LeaderboardEntry player(@PathVariable @Pattern(regexp = Ids.PATTERN, message = Ids.MESSAGE) String gameId,
			@PathVariable @Pattern(regexp = Ids.PATTERN, message = Ids.MESSAGE) String playerId) {
		return leaderboardCache.player(gameId, playerId)
			.orElseThrow(() -> new PlayerNotRankedException(gameId, playerId));
	}

	public record LeaderboardResponse(String gameId, List<LeaderboardEntry> entries) {
	}

}
