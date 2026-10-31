package dev.princemaurya.leaderboard.leaderboard;

import java.util.List;
import java.util.stream.IntStream;

import dev.princemaurya.leaderboard.score.PlayerScore;
import dev.princemaurya.leaderboard.score.PlayerScoreId;
import dev.princemaurya.leaderboard.score.PlayerScoreRepository;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LeaderboardRebuildServiceTests {

	private final PlayerScoreRepository repository = mock(PlayerScoreRepository.class);

	private final LeaderboardCache cache = mock(LeaderboardCache.class);

	private final LeaderboardRebuildService service = new LeaderboardRebuildService(repository, cache);

	@Test
	void pagesThroughAllPlayersUsingTheLastPlayerIdAsCursor() {
		List<PlayerScore> fullPage = scores(0, LeaderboardRebuildService.BATCH_SIZE);
		List<PlayerScore> lastPage = scores(LeaderboardRebuildService.BATCH_SIZE, 1);
		String lastIdOfFirstPage = fullPage.get(fullPage.size() - 1).getId().getPlayerId();
		given(repository.findPageAfter(eq("fc-arena"), eq(""), any(Limit.class))).willReturn(fullPage);
		given(repository.findPageAfter(eq("fc-arena"), eq(lastIdOfFirstPage), any(Limit.class))).willReturn(lastPage);

		long loaded = service.rebuild("fc-arena");

		assertThat(loaded).isEqualTo(LeaderboardRebuildService.BATCH_SIZE + 1L);
		verify(cache).setTotals("fc-arena", fullPage);
		verify(cache).setTotals("fc-arena", lastPage);
	}

	@Test
	void stopsAfterAShortPageWithoutAnExtraQuery() {
		List<PlayerScore> page = scores(0, 3);
		given(repository.findPageAfter(eq("fc-arena"), eq(""), any(Limit.class))).willReturn(page);

		assertThat(service.rebuild("fc-arena")).isEqualTo(3);
		verify(repository, never()).findPageAfter(eq("fc-arena"), eq("p-00002"), any(Limit.class));
	}

	@Test
	void emptyGameWritesNothing() {
		given(repository.findPageAfter(anyString(), anyString(), any(Limit.class))).willReturn(List.of());

		assertThat(service.rebuild("fc-arena")).isZero();
		verify(cache, never()).setTotals(anyString(), any());
	}

	private static List<PlayerScore> scores(int from, int count) {
		return IntStream.range(from, from + count).mapToObj(i -> {
			PlayerScore score = mock(PlayerScore.class);
			given(score.getId()).willReturn(new PlayerScoreId("fc-arena", "p-%05d".formatted(i)));
			return score;
		}).toList();
	}

}
