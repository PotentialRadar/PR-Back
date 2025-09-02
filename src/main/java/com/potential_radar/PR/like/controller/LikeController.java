package com.potential_radar.PR.like.controller;

import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.dto.LikeRequestDto;
import com.potential_radar.PR.like.dto.LikeResponseDto;
import com.potential_radar.PR.like.dto.LikeStatusResponseDto;
import com.potential_radar.PR.like.service.LikeService;
import com.potential_radar.PR.user.dto.portfolios.PortfolioSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<LikeResponseDto> toggleLike(
            @Valid @RequestBody LikeRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        LikeResponseDto responseDto = likeService.toggleLike(requestDto, username);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/portfolios")
    public ResponseEntity<List<PortfolioSummaryResponse>> getLikedPortfolios(@RequestParam Long userId) {
        List<PortfolioSummaryResponse> likedPortfolios = likeService.getLikedPortfolios(userId);
        return ResponseEntity.ok(likedPortfolios);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getLikeCount(
            @RequestParam TargetType targetType,
            @RequestParam Long targetId) {

        long likeCount = likeService.getLikeCount(targetType, targetId);
        return ResponseEntity.ok(likeCount);
    }

    @GetMapping("/status")
    public ResponseEntity<LikeStatusResponseDto> getLikeStatus(
            @RequestParam TargetType targetType,
            @RequestParam Long targetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isLiked = likeService.isLikedByUser(targetType, targetId, userDetails.getUsername());
        return ResponseEntity.ok(new LikeStatusResponseDto(isLiked));
    }
}
