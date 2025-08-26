package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.review.UserReceivedReviewResponse;
import com.potential_radar.PR.user.service.UserReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final UserReviewService userReviewService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<UserReceivedReviewResponse>> getUserReceivedReviews(@PathVariable Long userId) {
        List<UserReceivedReviewResponse> reviews = userReviewService.getReceivedReviews(userId);
        return ResponseEntity.ok(reviews);
    }
}