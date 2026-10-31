package dev.princemaurya.leaderboard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.princemaurya.leaderboard.config.LeaderboardProperties;
import dev.princemaurya.leaderboard.score.PlayerScore;
import dev.princemaurya.leaderboard.score.PlayerScoreId;
import dev.princemaurya.leaderboard.score.PlayerScoreRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LeaderboardIntegrationTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PlayerScoreRepository playerScores;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private LeaderboardProperties properties;

	@Autowired
	private ConsumerFactory<String, String> consumerFactory;

	@Test
	void rankedLeaderboardReflectsSubmittedMatches() throws Exception {
		String game = "game-" + shortId();
		submit(UUID.randomUUID(), game, "alice", 30, "bob", 50);
		submit(UUID.randomUUID(), game, "alice", 40, "carol", 20);

		await().atMost(TIMEOUT).untilAsserted(() -> mvc.perform(get("/api/v1/games/{game}/leaderboard", game))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries.length()").value(3))
			.andExpect(jsonPath("$.entries[0].playerId").value("alice"))
			.andExpect(jsonPath("$.entries[0].points").value(70))
			.andExpect(jsonPath("$.entries[1].playerId").value("bob"))
			.andExpect(jsonPath("$.entries[2].playerId").value("carol")));

		mvc.perform(get("/api/v1/games/{game}/leaderboard/players/{player}", game, "bob"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rank").value(2))
			.andExpect(jsonPath("$.points").value(50));
	}

	@Test
	void duplicateMatchIsCountedOnce() throws Exception {
		String game = "game-" + shortId();
		UUID matchId = UUID.randomUUID();
		submit(matchId, game, "alice", 25, "bob", 5);
		submit(matchId, game, "alice", 25, "bob", 5);
		// same partition, so this one is handled after both copies above
		submit(UUID.randomUUID(), game, "marker", 1, "marker-2", 1);

		await().atMost(TIMEOUT)
			.until(() -> playerScores.findById(new PlayerScoreId(game, "marker")).isPresent());

		PlayerScore alice = playerScores.findById(new PlayerScoreId(game, "alice")).orElseThrow();
		assertThat(alice.getTotalPoints()).isEqualTo(25);
		assertThat(alice.getMatchesPlayed()).isEqualTo(1);
	}

	@Test
	void malformedEventGoesToDeadLetterTopicWithoutBlockingLaterEvents() throws Exception {
		String game = "game-" + shortId();
		kafkaTemplate.send(properties.matchTopic(), game, "{this is not json").get();
		submit(UUID.randomUUID(), game, "alice", 10, "bob", 3);

		await().atMost(TIMEOUT)
			.until(() -> playerScores.findById(new PlayerScoreId(game, "alice")).isPresent());

		try (Consumer<String, String> consumer = consumerFactory.createConsumer("dlt-check-" + shortId(), null)) {
			consumer.subscribe(List.of(properties.deadLetterTopic()));
			List<String> deadLetters = new ArrayList<>();
			await().atMost(TIMEOUT).untilAsserted(() -> {
				for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
					if (game.equals(record.key())) {
						deadLetters.add(record.value());
					}
				}
				assertThat(deadLetters).containsExactly("{this is not json");
			});
		}
	}

	@Test
	void unknownPlayerReturnsNotFound() throws Exception {
		mvc.perform(get("/api/v1/games/{game}/leaderboard/players/{player}", "no-such-game", "nobody"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void rejectsOutOfRangeLimit() throws Exception {
		mvc.perform(get("/api/v1/games/{game}/leaderboard", "fc-arena").param("limit", "500"))
			.andExpect(status().isBadRequest());
	}

	private void submit(UUID matchId, String game, String p1, long points1, String p2, long points2)
			throws Exception {
		String body = """
				{"matchId":"%s","gameId":"%s","results":[
				  {"playerId":"%s","points":%d},{"playerId":"%s","points":%d}]}
				""".formatted(matchId, game, p1, points1, p2, points2);
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isAccepted());
	}

	private static String shortId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

}
