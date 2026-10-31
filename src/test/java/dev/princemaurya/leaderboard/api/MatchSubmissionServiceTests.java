package dev.princemaurya.leaderboard.api;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import dev.princemaurya.leaderboard.event.MatchCompletedEvent;
import dev.princemaurya.leaderboard.event.MatchEventPublisher;
import dev.princemaurya.leaderboard.event.PlayerPoints;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MatchSubmissionServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

	private final MatchEventPublisher publisher = mock(MatchEventPublisher.class);

	private final MatchSubmissionService service = new MatchSubmissionService(publisher,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void publishesEventWithAllResults() {
		UUID matchId = UUID.randomUUID();
		SubmitMatchRequest request = new SubmitMatchRequest(matchId, "fc-arena",
				List.of(new SubmitMatchRequest.Result("alice", 30L), new SubmitMatchRequest.Result("bob", 10L)));

		MatchCompletedEvent event = service.submit(request);

		assertThat(event.matchId()).isEqualTo(matchId);
		assertThat(event.completedAt()).isEqualTo(NOW);
		assertThat(event.results()).containsExactly(new PlayerPoints("alice", 30), new PlayerPoints("bob", 10));
		verify(publisher).publish(event);
	}

	@Test
	void rejectsSamePlayerTwiceInOneMatch() {
		SubmitMatchRequest request = new SubmitMatchRequest(UUID.randomUUID(), "fc-arena",
				List.of(new SubmitMatchRequest.Result("alice", 30L), new SubmitMatchRequest.Result("alice", 10L)));

		assertThatThrownBy(() -> service.submit(request)).isInstanceOf(InvalidMatchException.class)
			.hasMessageContaining("alice");
		verify(publisher, never()).publish(any());
	}

}
