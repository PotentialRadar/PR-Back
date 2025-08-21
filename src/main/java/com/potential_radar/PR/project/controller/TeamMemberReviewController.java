package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.project.dto.TeamMemberReviewRequestDto;
import com.potential_radar.PR.project.service.TeamMemberReviewService;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class TeamMemberReviewController {

    private final TeamMemberReviewService teamMemberReviewService;
    private final UserRepository userRepository;

    private String getUserEmailFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        return null;
    }

    @PostMapping("/member")
    public ResponseEntity<Void> createTeamMemberReview(
            @Valid @RequestBody TeamMemberReviewRequestDto request,
            Authentication authentication) {

        String userEmail = getUserEmailFromAuthentication(authentication);
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User reviewer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("DB에서 작성자 정보를 찾을 수 없습니다: " + userEmail));

        teamMemberReviewService.createReview(request, reviewer.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
