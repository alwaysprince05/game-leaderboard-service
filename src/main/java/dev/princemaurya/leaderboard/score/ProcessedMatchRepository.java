package dev.princemaurya.leaderboard.score;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedMatchRepository extends JpaRepository<ProcessedMatch, UUID> {

	// returns 0 if the match was already processed
	@Modifying
	@Query(value = """
			INSERT INTO processed_matches (match_id, game_id, processed_at)
			VALUES (:matchId, :gameId, now())
			ON CONFLICT (match_id) DO NOTHING
			""", nativeQuery = true)
	int markProcessed(@Param("matchId") UUID matchId, @Param("gameId") String gameId);

}
