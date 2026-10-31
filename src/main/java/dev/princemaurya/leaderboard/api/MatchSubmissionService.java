package dev.princemaurya.leaderboard.api;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.princemaurya.leaderboard.event.MatchCompletedEvent;
import dev.princemaurya.leaderboard.event.MatchEventPublisher;
import dev.princemaurya.leaderboard.event.PlayerPoints;

import org.springframework.stereotype.Service;

@Service
public class MatchSubmissionService {

	private final MatchEventPublisher publisher;

	private final Clock clock;

	public MatchSubmissionService(MatchEventPublisher publisher, Clock clock) {
		this.publisher = publisher;
		this.clock = clock;
	}

	public MatchCompletedEvent submit(SubmitMatchRequest request) {
		Set<String> seen = new HashSet<>();
		for (SubmitMatchRequest.Result result : request.results()) {
			if (!seen.add(result.playerId())) {
				throw new InvalidMatchException("Player '" + result.playerId() + "' appears more than once");
			}
		}
		List<PlayerPoints> results = request.results()
			.stream()
			.map(result -> new PlayerPoints(result.playerId(), result.points()))
			.toList();
		MatchCompletedEvent event = new MatchCompletedEvent(request.matchId(), request.gameId(), results,
				Instant.now(clock));
		publisher.publish(event);
		return event;
	}

}
