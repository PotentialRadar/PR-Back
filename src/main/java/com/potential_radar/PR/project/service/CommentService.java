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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return convertToDto(savedComment, new ArrayList<>(), userId, project.getTeamLeader().getUserId());
    }

    // 댓글 목록 조회 (계층 구조)
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long projectId, Long currentUserId) {
        ProjectRecruitment project = projectRepository.findById(projectId)
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
            boolean canView = currentUserId != null && (currentUserId.equals(comment.getUser().getUserId()) || currentUserId.equals(teamLeaderId));
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
            boolean canView = currentUserId != null && (currentUserId.equals(comment.getUser().getUserId()) || currentUserId.equals(teamLeaderId));
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
