package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.project.dto.ProjectRecruitmentRequest;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.project.service.ProjectRecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectRecruitmentController {
    private final ProjectRecruitmentService projectRecruitmentService;

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

    // 파일 업로드
    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/files/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = new File(uploadDir + fileName);
            dest.getParentFile().mkdirs();
            file.transferTo(dest);
            return ResponseEntity.ok("/uploads/files/" + fileName);
        } catch (Exception e) {
            e.printStackTrace();  // 로그로 남기기
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 업로드 실패: " + e.getMessage());
        }
    }

    // 파일 다운로드
    @GetMapping("/download-file")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) throws IOException {
        File file = new File("uploads/files/" + filePath);
        if (!file.exists()) throw new FileNotFoundException("파일이 존재하지 않습니다.");
        Resource resource = new InputStreamResource(new FileInputStream(file));
        String contentType = Files.probeContentType(file.toPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(resource);
    }
}
