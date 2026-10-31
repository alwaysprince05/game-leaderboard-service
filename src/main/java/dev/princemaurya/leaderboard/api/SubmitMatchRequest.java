package dev.princemaurya.leaderboard.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitMatchRequest(
		@NotNull UUID matchId,
		@NotNull @Pattern(regexp = Ids.PATTERN, message = Ids.MESSAGE) String gameId,
		@NotEmpty @Size(max = 100) List<@NotNull @Valid Result> results) {

	public record Result(
			@NotNull @Pattern(regexp = Ids.PATTERN, message = Ids.MESSAGE) String playerId,
			@NotNull @Min(0) @Max(1_000_000) Long points) {
	}

}
