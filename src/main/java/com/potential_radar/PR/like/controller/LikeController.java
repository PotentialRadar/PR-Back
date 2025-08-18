package com.potential_radar.PR.like.controller;

import com.potential_radar.PR.like.dto.LikeRequestDto;
import com.potential_radar.PR.like.dto.LikeResponseDto;
import com.potential_radar.PR.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

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
}
