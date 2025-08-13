package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.search.dto.*;
import com.potential_radar.PR.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/users")
    public ResponseEntity<SearchResult<UserSearchRes>> searchUsers(
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String techPart,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UserSearchReq request = UserSearchReq.builder()
                .nickname(nickname)
                .techPart(techPart)
                .techStacks(techStacks)
                .page(page)
                .size(size)
                .build();

        SearchResult<UserSearchRes> result = searchService.searchUsers(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/projects")
    public ResponseEntity<SearchResult<ProjectSearchRes>> searchProjects(
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) List<String> requiredTechParts,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProjectSearchReq request = ProjectSearchReq.builder()
                .projectName(projectName)
                .techStacks(techStacks)
                .requiredTechParts(requiredTechParts)
                .page(page)
                .size(size)
                .build();

        SearchResult<ProjectSearchRes> result = searchService.searchProjects(request);
        return ResponseEntity.ok(result);
    }
}
