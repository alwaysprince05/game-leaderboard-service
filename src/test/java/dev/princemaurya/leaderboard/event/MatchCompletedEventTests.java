package dev.princemaurya.leaderboard.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchCompletedEventTests {

	@Test
	void acceptsCompleteEvent() {
		MatchCompletedEvent event = new MatchCompletedEvent(UUID.randomUUID(), "fc-arena",
				List.of(new PlayerPoints("alice", 5)), Instant.now());
		assertThatCode(event::validate).doesNotThrowAnyException();
	}

	@Test
	void rejectsMissingMatchId() {
		MatchCompletedEvent event = new MatchCompletedEvent(null, "fc-arena", List.of(new PlayerPoints("alice", 5)),
				Instant.now());
		assertThatThrownBy(event::validate).isInstanceOf(InvalidEventException.class);
	}

	@Test
	void rejectsNegativePoints() {
		MatchCompletedEvent event = new MatchCompletedEvent(UUID.randomUUID(), "fc-arena",
				List.of(new PlayerPoints("alice", -1)), Instant.now());
		assertThatThrownBy(event::validate).isInstanceOf(InvalidEventException.class);
	}

}
