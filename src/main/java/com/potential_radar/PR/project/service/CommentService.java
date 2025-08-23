package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.exception.AccessDeniedException;
import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.project.domain.ProjectComment;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.dto.CommentRequestDto;
import com.potential_radar.PR.project.dto.CommentResponseDto;
import com.potential_radar.PR.project.repository.ProjectCommentRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.notification.service.NotificationService;
import com.potential_radar.PR.notification.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final ProjectCommentRepository commentRepository;
    private final ProjectRecruitmentRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 댓글 생성
    public CommentResponseDto createComment(Long projectId, Long userId, CommentRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        ProjectRecruitment project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        ProjectComment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("부모 댓글을 찾을 수 없습니다."));
        }

        boolean secret = Boolean.TRUE.equals(request.getIsPrivate());
        ProjectComment comment = ProjectComment.builder()
                .content(request.getContent())
                .isPrivate(secret)
                .user(user)
                .project(project)
                .parent(parentComment)
                .build();

        ProjectComment savedComment = commentRepository.save(comment);
        
        // 댓글 저장 후 트랜잭션 커밋되면 알림 전송
        return convertToDto(savedComment, new ArrayList<>(), userId, project.getTeamLeader().getUserId());
    }

    // 댓글 생성과 알림 전송을 분리 (트랜잭션 커밋 후 알림 전송)
    @Transactional
    public CommentResponseDto createCommentWithNotification(Long projectId, Long userId, CommentRequestDto request) {
        // 1단계: 댓글 생성 (트랜잭션 내)
        CommentResponseDto result = createComment(projectId, userId, request);
        
        // 2단계: 댓글 저장이 성공하면 알림 전송 로직 (같은 트랜잭션 내)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        ProjectRecruitment project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        ProjectComment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("부모 댓글을 찾을 수 없습니다."));
        }

        // 알림 전송 로직
        if (!user.getUserId().equals(project.getTeamLeader().getUserId())) {
            // 프로젝트 리더에게 알림 전송
            String notificationContent = String.format("%s님이 '%s' 프로젝트에 댓글을 남겼습니다.", 
                    user.getNickname(), project.getTitle());
            String url = String.format("/projects/%d", projectId);
            
            // 트랜잭션 커밋 후 알림 전송하도록 수정
            sendNotificationAfterCommit(
                    project.getTeamLeader(),
                    NotificationType.COMMENT,
                    notificationContent,
                    url
            );
        }
        
        // 대댓글인 경우 부모 댓글 작성자에게도 알림 전송
        if (parentComment != null && 
            !user.getUserId().equals(parentComment.getUser().getUserId()) &&
            !parentComment.getUser().getUserId().equals(project.getTeamLeader().getUserId())) {
            
            String replyNotificationContent = String.format("%s님이 회원님의 댓글에 답글을 달았습니다.", 
                    user.getNickname());
            String url = String.format("/projects/%d", projectId);
            
            sendNotificationAfterCommit(
                    parentComment.getUser(),
                    NotificationType.COMMENT,
                    replyNotificationContent,
                    url
            );
        }
        
        return result;
    }

    // 트랜잭션 커밋 후 알림 전송하는 메서드
    private void sendNotificationAfterCommit(User receiver, NotificationType type, String content, String url) {
        // Spring의 TransactionSynchronization을 사용하여 트랜잭션 커밋 후 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notificationService.send(receiver, type, content, url, null, LocalDateTime.now());
                } catch (Exception e) {
                    // 알림 전송 실패해도 댓글 생성에는 영향 없도록 로그만 남김
                    System.err.println("알림 전송 실패: " + e.getMessage());
                }
            }
        });
    }

    // 댓글 목록 조회 (계층 구조)
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long projectId, Long currentUserId) {
        ProjectRecruitment project = projectRepository.findByIdWithTeamLeader(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));
        Long teamLeaderId = project.getTeamLeader().getUserId();

        List<ProjectComment> comments = commentRepository.findByProject_ProjectId(projectId);
        Map<ProjectComment, List<ProjectComment>> parentToChildrenMap = comments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(ProjectComment::getParent));

        return comments.stream()
                .filter(c -> c.getParent() == null) // Root 댓글만 필터링
                .map(rootComment -> convertToDto(rootComment, parentToChildrenMap, currentUserId, teamLeaderId))
                .collect(Collectors.toList());
    }

    // 댓글 수정
    public void updateComment(Long commentId, Long userId, CommentRequestDto request) {
        ProjectComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("댓글 수정 권한이 없습니다.");
        }

        // 수정 (넘어온 값이 있을 때만 변경)
        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }
        if (request.getIsPrivate() != null) {
            comment.setPrivate(request.getIsPrivate());
        }
    }

    // 댓글 삭제
    public void deleteComment(Long commentId, Long userId) {
        ProjectComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("댓글 삭제 권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }

    // --- Helper Methods ---

    private CommentResponseDto convertToDto(ProjectComment comment, Map<ProjectComment, List<ProjectComment>> parentToChildrenMap, Long currentUserId, Long teamLeaderId) {
        List<ProjectComment> childrenEntities = parentToChildrenMap.getOrDefault(comment, new ArrayList<>());
        List<CommentResponseDto> childrenDtos = childrenEntities.stream()
                .map(child -> convertToDto(child, parentToChildrenMap, currentUserId, teamLeaderId))
                .collect(Collectors.toList());

        String content = comment.getContent();
        if (comment.isPrivate()) {
            boolean canView = currentUserId != null && (
                currentUserId.equals(comment.getUser().getUserId()) || // 현재 사용자가 이 댓글의 작성자인 경우
                currentUserId.equals(teamLeaderId) || // 현재 사용자가 프로젝트 팀 리더인 경우
                (comment.getParent() != null && currentUserId.equals(comment.getParent().getUser().getUserId())) // 현재 사용자가 부모 댓글의 작성자인 경우
            );
            if (!canView) {
                content = "비밀 댓글입니다.";
            }
        }

        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .userId(comment.getUser().getUserId())
                .nickname(comment.getUser().getNickname())
                .content(content)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isPrivate(comment.isPrivate())
                .children(childrenDtos)
                .build();
    }

    private CommentResponseDto convertToDto(ProjectComment comment, List<CommentResponseDto> children, Long currentUserId, Long teamLeaderId) {
        String content = comment.getContent();
        if (comment.isPrivate()) {
            boolean canView = currentUserId != null && (
                currentUserId.equals(comment.getUser().getUserId()) || // 현재 사용자가 이 댓글의 작성자인 경우
                currentUserId.equals(teamLeaderId) || // 현재 사용자가 프로젝트 팀 리더인 경우
                (comment.getParent() != null && currentUserId.equals(comment.getParent().getUser().getUserId())) // 현재 사용자가 부모 댓글의 작성자인 경우
            );
            if (!canView) {
                content = "비밀 댓글입니다.";
            }
        }

        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .userId(comment.getUser().getUserId())
                .nickname(comment.getUser().getNickname())
                .content(content)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isPrivate(comment.isPrivate())
                .children(children)
                .build();
    }
}
