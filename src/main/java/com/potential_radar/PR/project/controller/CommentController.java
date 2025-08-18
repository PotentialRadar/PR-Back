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

        // Long userId = userDetails.getUser().getUserId(); // 기존 코드 주석 처리

        // ===================== 테스트를 위한 임시 코드 =====================
        // WebSecurityConfig가 확정되기 전까지 댓글 생성 기능 테스트를 위한 임시 로직입니다.
        Long userId;
        if (userDetails == null) {
            // 현재 WebSecurityConfig의 permitAll() 설정 때문에 인증 정보가 없어 userDetails가 null이므로,
            // 테스트를 위해 사용자 ID를 1L로 강제 설정합니다.
            System.out.println("--- [테스트 경고] 인증 정보가 없어 임시로 사용자 ID 1번으로 댓글을 작성합니다. ---");
            userId = 1L; // DB에 존재하는 사용자 ID로 설정해주세요.
        } else {
            // 토큰 인증이 정상적으로 동작할 경우 기존 로직을 그대로 사용합니다.
            userId = userDetails.getUser().getUserId();
        }
        // =================================================================

        CommentResponseDto createdComment = commentService.createComment(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Long currentUserId = (userDetails != null) ? userDetails.getUser().getUserId() : null; // 기존 코드 주석 처리

        // ===================== 테스트를 위한 임시 코드 =====================
        Long currentUserId;
        if (userDetails == null) {
            System.out.println("--- [테스트 경고] 인증 정보가 없어 임시로 사용자 ID 1번으로 댓글을 조회합니다. ---");
            currentUserId = 1L; // DB에 존재하는 사용자 ID로 설정해주세요.
        } else {
            currentUserId = userDetails.getUser().getUserId();
        }
        // =================================================================

        List<CommentResponseDto> comments = commentService.getComments(projectId, currentUserId);
        return ResponseEntity.ok(comments);
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Long userId = userDetails.getUser().getUserId(); // 기존 코드 주석 처리

        // ===================== 테스트를 위한 임시 코드 =====================
        Long userId;
        if (userDetails == null) {
            System.out.println("--- [테스트 경고] 인증 정보가 없어 임시로 사용자 ID 1번으로 댓글을 수정합니다. ---");
            userId = 1L; // DB에 존재하는 사용자 ID로 설정해주세요.
        } else {
            userId = userDetails.getUser().getUserId();
        }
        // =================================================================

        commentService.updateComment(commentId, userId, request);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Long userId = userDetails.getUser().getUserId(); // 기존 코드 주석 처리

        // ===================== 테스트를 위한 임시 코드 =====================
        Long userId;
        if (userDetails == null) {
            System.out.println("--- [테스트 경고] 인증 정보가 없어 임시로 사용자 ID 1번으로 댓글을 삭제합니다. ---");
            userId = 1L; // DB에 존재하는 사용자 ID로 설정해주세요.
        } else {
            userId = userDetails.getUser().getUserId();
        }
        // =================================================================

        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }
}
