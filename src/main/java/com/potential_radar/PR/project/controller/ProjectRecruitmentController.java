package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.common.S3.S3Uploader;
import com.potential_radar.PR.common.exception.NotFoundException;
// import com.potential_radar.PR.project.dto.ProjectAttachmentDto;
import com.potential_radar.PR.project.dto.ProjectMemberResponseDTO;
import com.potential_radar.PR.project.dto.ProjectRecruitmentRequest;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.project.dto.ProjectStatusUpdateRequest;
import com.potential_radar.PR.project.service.ProjectRecruitmentService;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectRecruitmentController {
    private final ProjectRecruitmentService projectRecruitmentService;
    private final S3Uploader s3Uploader;
    private final UserRepository userRepository;

    // 허용된 파일 확장자 및 MIME 타입 정의
    private static final Set<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "pdf")
            .stream().collect(Collectors.toSet());
    private static final Set<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "application/pdf")
            .stream().collect(Collectors.toSet());

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
        return null;
    }

    // 구인글 등록
    @PostMapping
    public ResponseEntity<Long> createProject(@Valid @RequestBody ProjectRecruitmentRequest request, Authentication authentication) {
        String userEmail = getUserEmailFromAuthentication(authentication);
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("DB에서 사용자 정보를 찾을 수 없습니다: " + userEmail));

        Long id = projectRecruitmentService.createProject(request, user);
        return ResponseEntity.ok(id);
    }

    // 프로젝트 전체 조회
    @GetMapping
    public ResponseEntity<Page<ProjectRecruitmentResponse>> getAllProjects(Authentication authentication, Pageable pageable) {
        String userEmail = getUserEmailFromAuthentication(authentication);
        Page<ProjectRecruitmentResponse> response = projectRecruitmentService.getAllProjects(userEmail, pageable);
        return ResponseEntity.ok(response);
    }

    // 구인글 단일 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProjectRecruitmentResponse> getProject(@PathVariable Long id, Authentication authentication) {
        String userEmail = getUserEmailFromAuthentication(authentication);
        ProjectRecruitmentResponse response = projectRecruitmentService.getProject(id, userEmail);
        return ResponseEntity.ok(response);
    }

    // 사용자가 생성한 프로젝트 목록 조회
    @GetMapping("/users/{userId}/created")
    public ResponseEntity<List<ProjectRecruitmentResponse>> getProjectsCreatedByUser(@PathVariable Long userId) {
        List<ProjectRecruitmentResponse> response = projectRecruitmentService.getProjectsCreatedByUser(userId);
        return ResponseEntity.ok(response);
    }

    // 구인글 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectRecruitmentRequest request
    ) {
        projectRecruitmentService.updateProject(id, request);
        return ResponseEntity.ok().build();
    }

    // 구인글 상태 변경
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateProjectStatus(
            @PathVariable Long id,
            @RequestBody ProjectStatusUpdateRequest request,
            @RequestParam Long userId) {
        projectRecruitmentService.updateProjectStatus(id, request.getStatus(), userId);
        return ResponseEntity.ok().build();
    }

    // 확정된 프로젝트 멤버 목록 조회
    @GetMapping("/{projectId}/confirmed-members") // New endpoint
    public ResponseEntity<List<ProjectMemberResponseDTO>> getConfirmedProjectMembers(
            @PathVariable Long projectId,
            Authentication authentication) {

        String userEmail = getUserEmailFromAuthentication(authentication);
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("DB에서 사용자 정보를 찾을 수 없습니다: " + userEmail));
        Long currentUserId = currentUser.getUserId();

        List<ProjectMemberResponseDTO> members = projectRecruitmentService.getConfirmedProjectMembers(projectId, currentUserId);
        return ResponseEntity.ok(members);
    }

    // 구인글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectRecruitmentService.deleteProject(id);
        return ResponseEntity.ok().build();
    }
    // S3 파일 업로드
//    @PostMapping("/upload-file")
//    public ResponseEntity<ProjectAttachmentDto> uploadFile(@RequestParam("file") MultipartFile file) {
//        try {
//            // 1. 파일 유효성 검사
//            String originalFilename = file.getOriginalFilename();
//            String fileExtension = "";
//            if (originalFilename != null && originalFilename.contains(".")) {
//                fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
//            }
//
//            String contentType = file.getContentType();
//
//            if (!ALLOWED_EXTENSIONS.contains(fileExtension) || !ALLOWED_MIME_TYPES.contains(contentType)) {
//                // 적절한 에러 메시지를 포함한 응답 반환
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); 
//            }
//
//            // 2. S3 업로드
//            String fileUrl = s3Uploader.upload(file, "project-files");
//            
//            // 3. ProjectAttachmentDto 객체 생성 및 반환
//            ProjectAttachmentDto attachmentDto = ProjectAttachmentDto.builder()
//                    .name(originalFilename) // 원본 파일명 사용
//                    .url(fileUrl)
//                    .size(file.getSize())
//                    .build();
//            
//            return ResponseEntity.ok(attachmentDto);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.internalServerError().body(null); // 에러 발생 시 null 반환 또는 적절한 에러 DTO 반환
//        }
//    }
}
