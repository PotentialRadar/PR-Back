package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.project.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    //지원
    @PostMapping("/{projectId}/apply")
    public ResponseEntity<Long> ApplyToProject(@PathVariable Long projectId, @RequestParam Long userId){
        Long id = projectMemberService.ApplytoProject(projectId, userId);
        return ResponseEntity.ok(id);

    }

}
