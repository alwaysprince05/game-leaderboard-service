package dev.princemaurya.leaderboard.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

	private final MatchSubmissionService submissionService;

	public MatchController(MatchSubmissionService submissionService) {
		this.submissionService = submissionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public SubmitMatchResponse submit(@Valid @RequestBody SubmitMatchRequest request) {
		submissionService.submit(request);
		return new SubmitMatchResponse(request.matchId(), "ACCEPTED");
	}

	public record SubmitMatchResponse(UUID matchId, String status) {
	}

}
