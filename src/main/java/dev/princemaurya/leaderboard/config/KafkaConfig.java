package dev.princemaurya.leaderboard.config;

import dev.princemaurya.leaderboard.event.InvalidEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import tools.jackson.core.JacksonException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaConfig {

	private static final int PARTITIONS = 3;

	@Bean
	NewTopic matchTopic(LeaderboardProperties properties) {
		return TopicBuilder.name(properties.matchTopic()).partitions(PARTITIONS).replicas(1).build();
	}

	@Bean
	NewTopic matchDeadLetterTopic(LeaderboardProperties properties) {
		return TopicBuilder.name(properties.deadLetterTopic()).partitions(1).replicas(1).build();
	}

	@Bean
	DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
			LeaderboardProperties properties) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
				(record, exception) -> new TopicPartition(properties.deadLetterTopic(), -1));
		// 3 retries, then send to the DLT
		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
		handler.addNotRetryableExceptions(InvalidEventException.class, JacksonException.class);
		return handler;
	}

}
