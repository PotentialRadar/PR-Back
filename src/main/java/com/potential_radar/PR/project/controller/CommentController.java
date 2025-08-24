package com.potential_radar.PR.project.controller;

// 필요한 import 문들
import com.potential_radar.PR.project.dto.CommentRequestDto;
import com.potential_radar.PR.project.dto.CommentResponseDto;
import com.potential_radar.PR.project.service.CommentService;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Authentication import
import org.springframework.security.core.userdetails.UserDetails; // UserDetails import
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/projects/{projectId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    /**
     * Authentication 객체에서 사용자 이메일을 추출하는 헬퍼 메서드
     */
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

        return null; // 알 수 없는 타입이거나, 인증되지 않은 사용자(anonymous)
    }

    // 댓글 생성
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long projectId,
            @RequestBody CommentRequestDto request,
            Authentication authentication) { // Authentication 객체를 직접 받기

        String userEmail = getUserEmailFromAuthentication(authentication);
        if (userEmail == null) {
            // 이메일을 얻을 수 없으면, 인증되지 않은 사용자로 간주
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("DB에서 사용자를 찾을 수 없습니다: " + userEmail));
        Long userId = user.getUserId();

        CommentResponseDto createdComment = commentService.createCommentWithNotification(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long projectId,
            Authentication authentication) { // 비로그인 사용자도 조회 가능하므로, authentication은 null일 수 있음

        String userEmail = getUserEmailFromAuthentication(authentication);
        Long currentUserId = null;

        if (userEmail != null) {
            currentUserId = userRepository.findByEmail(userEmail)
                    .map(User::getUserId)
                    .orElse(null); // DB에 해당 이메일이 없을 수도 있는 엣지 케이스 처리
        }

        List<CommentResponseDto> comments = commentService.getComments(projectId, currentUserId);
        return ResponseEntity.ok(comments);
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto request,
            Authentication authentication) {

        String userEmail = getUserEmailFromAuthentication(authentication);
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("DB에서 사용자를 찾을 수 없습니다: " + userEmail));
        Long userId = user.getUserId();

        commentService.updateComment(commentId, userId, request);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        String userEmail = getUserEmailFromAuthentication(authentication);
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("DB에서 사용자를 찾을 수 없습니다: " + userEmail));
        Long userId = user.getUserId();

        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }
}
