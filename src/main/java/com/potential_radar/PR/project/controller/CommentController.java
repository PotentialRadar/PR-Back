package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.config.oauth.CustomUserDetails;
import com.potential_radar.PR.project.dto.CommentRequestDto;
import com.potential_radar.PR.project.dto.CommentResponseDto;
import com.potential_radar.PR.project.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 생성
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long projectId,
            @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        CommentResponseDto createdComment = commentService.createComment(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long currentUserId = (userDetails != null) ? userDetails.getUser().getUserId() : null;
        List<CommentResponseDto> comments = commentService.getComments(projectId, currentUserId);
        return ResponseEntity.ok(comments);
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        commentService.updateComment(commentId, userId, request);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }
}
