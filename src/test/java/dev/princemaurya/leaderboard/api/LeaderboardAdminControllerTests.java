package dev.princemaurya.leaderboard.api;

import dev.princemaurya.leaderboard.leaderboard.LeaderboardRebuildService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaderboardAdminController.class)
class LeaderboardAdminControllerTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private LeaderboardRebuildService rebuildService;

	@Test
	void returnsNumberOfPlayersLoaded() throws Exception {
		given(rebuildService.rebuild("fc-arena")).willReturn(42L);

		mvc.perform(post("/api/v1/admin/games/{game}/leaderboard/rebuild", "fc-arena"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.gameId").value("fc-arena"))
			.andExpect(jsonPath("$.playersLoaded").value(42));
	}

	@Test
	void rejectsInvalidGameId() throws Exception {
		mvc.perform(post("/api/v1/admin/games/{game}/leaderboard/rebuild", "FC Arena!"))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(rebuildService);
	}

}
