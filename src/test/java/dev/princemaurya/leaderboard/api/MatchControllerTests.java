package dev.princemaurya.leaderboard.api;

import dev.princemaurya.leaderboard.event.EventPublishException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
class MatchControllerTests {

	private static final String VALID_BODY = """
			{"matchId":"8b0c1f8e-3f5a-4a39-9f35-5d4a1c2b7e10","gameId":"fc-arena",
			 "results":[{"playerId":"alice","points":30},{"playerId":"bob","points":10}]}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private MatchSubmissionService submissionService;

	@Test
	void acceptsValidMatch() throws Exception {
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.matchId").value("8b0c1f8e-3f5a-4a39-9f35-5d4a1c2b7e10"))
			.andExpect(jsonPath("$.status").value("ACCEPTED"));
	}

	@Test
	void rejectsMissingMatchId() throws Exception {
		String body = """
				{"gameId":"fc-arena","results":[{"playerId":"alice","points":30}]}
				""";
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsInvalidGameId() throws Exception {
		String body = VALID_BODY.replace("fc-arena", "FC Arena!");
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsNegativePoints() throws Exception {
		String body = VALID_BODY.replace("\"points\":30", "\"points\":-5");
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Request validation failed"))
			.andExpect(jsonPath("$.errors['results[0].points']").value("must be greater than or equal to 0"));
	}

	@Test
	void rejectsEmptyResults() throws Exception {
		String body = """
				{"matchId":"8b0c1f8e-3f5a-4a39-9f35-5d4a1c2b7e10","gameId":"fc-arena","results":[]}
				""";
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void returnsProblemDetailForDuplicatePlayers() throws Exception {
		willThrow(new InvalidMatchException("Player 'alice' appears more than once")).given(submissionService)
			.submit(any());
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Player 'alice' appears more than once"));
	}

	@Test
	void returnsServiceUnavailableWhenKafkaIsDown() throws Exception {
		willThrow(new EventPublishException("down", new RuntimeException())).given(submissionService).submit(any());
		mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.detail").value("The match could not be queued. Retry with the same matchId."));
	}

}
