package dev.princemaurya.leaderboard.event;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.princemaurya.leaderboard.config.LeaderboardProperties;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MatchEventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;

	private final JsonMapper jsonMapper;

	private final LeaderboardProperties properties;

	public MatchEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper,
			LeaderboardProperties properties) {
		this.kafkaTemplate = kafkaTemplate;
		this.jsonMapper = jsonMapper;
		this.properties = properties;
	}

	public void publish(MatchCompletedEvent event) {
		String payload = jsonMapper.writeValueAsString(event);
		try {
			// key = gameId so one game's events stay in order on one partition
			kafkaTemplate.send(properties.matchTopic(), event.gameId(), payload)
				.get(properties.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new EventPublishException("Interrupted while publishing match " + event.matchId(), ex);
		}
		catch (ExecutionException | TimeoutException ex) {
			throw new EventPublishException("Could not publish match " + event.matchId(), ex);
		}
	}

}
