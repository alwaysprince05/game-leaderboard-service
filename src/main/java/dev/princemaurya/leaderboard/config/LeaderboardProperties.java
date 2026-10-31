package dev.princemaurya.leaderboard.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "leaderboard")
public record LeaderboardProperties(@NotBlank String matchTopic, @NotNull Duration publishTimeout) {

	public String deadLetterTopic() {
		return matchTopic + ".dlt";
	}

}
