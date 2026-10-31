package dev.princemaurya.leaderboard.api;

import dev.princemaurya.leaderboard.leaderboard.LeaderboardRebuildService;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/games/{gameId}/leaderboard")
public class LeaderboardAdminController {

	private final LeaderboardRebuildService rebuildService;

	public LeaderboardAdminController(LeaderboardRebuildService rebuildService) {
		this.rebuildService = rebuildService;
	}

	@PostMapping("/rebuild")
	public RebuildResponse rebuild(@PathVariable @Pattern(regexp = Ids.PATTERN, message = Ids.MESSAGE) String gameId) {
		return new RebuildResponse(gameId, rebuildService.rebuild(gameId));
	}

	public record RebuildResponse(String gameId, long playersLoaded) {
	}

}
