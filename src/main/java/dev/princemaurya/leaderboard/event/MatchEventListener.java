package dev.princemaurya.leaderboard.event;

import dev.princemaurya.leaderboard.leaderboard.LeaderboardCache;
import dev.princemaurya.leaderboard.score.ScoreService;
import dev.princemaurya.leaderboard.score.ScoreService.ApplyResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MatchEventListener {

	private static final Logger log = LoggerFactory.getLogger(MatchEventListener.class);

	private final JsonMapper jsonMapper;

	private final ScoreService scoreService;

	private final LeaderboardCache leaderboardCache;

	private final Counter processed;

	private final Counter duplicates;

	private final Timer processingTimer;

	public MatchEventListener(JsonMapper jsonMapper, ScoreService scoreService, LeaderboardCache leaderboardCache,
			MeterRegistry registry) {
		this.jsonMapper = jsonMapper;
		this.scoreService = scoreService;
		this.leaderboardCache = leaderboardCache;
		this.processed = Counter.builder("leaderboard.matches.processed")
			.description("Match events applied to player scores")
			.register(registry);
		this.duplicates = Counter.builder("leaderboard.matches.duplicate")
			.description("Match events skipped because they were already applied")
			.register(registry);
		this.processingTimer = Timer.builder("leaderboard.matches.processing")
			.description("Time to apply one match event")
			.register(registry);
	}

	@KafkaListener(topics = "${leaderboard.match-topic}")
	public void onMatchCompleted(String payload) {
		MatchCompletedEvent event = jsonMapper.readValue(payload, MatchCompletedEvent.class);
		event.validate();
		processingTimer.record(() -> {
			// db commits first, then redis gets the totals (safe to redo on retry)
			ApplyResult result = scoreService.apply(event);
			leaderboardCache.setTotals(event.gameId(), result.totals());
			if (result.duplicate()) {
				duplicates.increment();
				log.info("Skipped duplicate match {} for game {}", event.matchId(), event.gameId());
			}
			else {
				processed.increment();
				log.debug("Applied match {} for game {}", event.matchId(), event.gameId());
			}
		});
	}

}
