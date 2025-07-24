package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.common.S3.S3Uploader;
import com.potential_radar.PR.project.dto.ProjectRecruitmentRequest;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.project.service.ProjectRecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectRecruitmentController {
    private final ProjectRecruitmentService projectRecruitmentService;
    private final S3Uploader s3Uploader;

    // 구인글 등록
    @PostMapping
    public ResponseEntity<Long> createProject(@RequestBody ProjectRecruitmentRequest request) {
        Long id = projectRecruitmentService.createProject(request);
        return ResponseEntity.ok(id);
    }

    // 구인글 단일 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProjectRecruitmentResponse> getProject(@PathVariable Long id) {
        ProjectRecruitmentResponse response = projectRecruitmentService.getProject(id);
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

    // 구인글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectRecruitmentService.deleteProject(id);
        return ResponseEntity.ok().build();
    }
    // S3 파일 업로드
    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // "project-files"는 S3 내 폴더(접두사) 없으면 자동 생성됨
            String fileUrl = s3Uploader.upload(file, "project-files");
            return ResponseEntity.ok(fileUrl); // 실제 접근 가능한 S3 URL 반환
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("파일 업로드 실패: " + e.getMessage());
        }
    }
}
