package dev.princemaurya.leaderboard.score;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerScoreRepository extends JpaRepository<PlayerScore, PlayerScoreId> {

	// atomic upsert, no read-modify-write
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO player_scores (game_id, player_id, total_points, matches_played, updated_at)
			VALUES (:gameId, :playerId, :points, 1, now())
			ON CONFLICT (game_id, player_id) DO UPDATE
			SET total_points = player_scores.total_points + EXCLUDED.total_points,
			    matches_played = player_scores.matches_played + 1,
			    updated_at = now()
			""", nativeQuery = true)
	int addPoints(@Param("gameId") String gameId, @Param("playerId") String playerId, @Param("points") long points);

	List<PlayerScore> findByIdGameIdAndIdPlayerIdIn(String gameId, Collection<String> playerIds);

	// keyset page by player id: stable even while new players are being inserted
	@Query("SELECT s FROM PlayerScore s WHERE s.id.gameId = :gameId AND s.id.playerId > :afterPlayerId ORDER BY s.id.playerId")
	List<PlayerScore> findPageAfter(@Param("gameId") String gameId, @Param("afterPlayerId") String afterPlayerId,
			Limit limit);

}
