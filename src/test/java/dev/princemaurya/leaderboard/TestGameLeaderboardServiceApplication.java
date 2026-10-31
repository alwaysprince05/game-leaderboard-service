package dev.princemaurya.leaderboard;

import org.springframework.boot.SpringApplication;

public class TestGameLeaderboardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(GameLeaderboardServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
