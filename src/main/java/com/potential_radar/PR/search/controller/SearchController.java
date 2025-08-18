package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.dto.*;
import com.potential_radar.PR.search.service.SearchService;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchService searchService;
    private final UserSearchRepository userSearchRepository;

    @GetMapping("/users")
    public ResponseEntity<SearchResult<UserSearchRes>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String nickname, // nickname 파라미터 추가
            @RequestParam(required = false) List<String> techParts,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) List<ExperienceRange> experienceRanges,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // nickname 파라미터가 있으면 keyword로 사용
        String searchKeyword = (nickname != null && !nickname.trim().isEmpty()) ? nickname : keyword;

        UserSearchReq request = UserSearchReq.builder()
                .keyword(searchKeyword)
                .techParts(techParts)
                .techStacks(techStacks)
                .experienceRanges(experienceRanges)
                .page(page)
                .size(size)
                .build();

        SearchResult<UserSearchRes> result = searchService.searchUsers(request);
        return ResponseEntity.ok(result);
    }

    
    // 테스트용 엔드포인트
    @GetMapping("/test/count")
    public ResponseEntity<Map<String, Object>> testCount() {
        long count = searchService.countAllUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    @GetMapping("/test/all")
    public ResponseEntity<Map<String, Object>> testFindAll() {
        Iterable<UserSearchDocument> users = searchService.findAllUsers();
        List<UserSearchDocument> userList = new ArrayList<>();
        users.forEach(userList::add);
        return ResponseEntity.ok(Map.of("users", userList, "size", userList.size()));
    }

    // 프로젝트 검색 엔드포인트
    @GetMapping("/projects")
    public ResponseEntity<SearchResult<ProjectSearchRes>> searchProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> techParts,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword(keyword)
                .techParts(techParts)
                .techStacks(techStacks)
                .statuses(statuses)
                .page(page)
                .size(size)
                .build();

        SearchResult<ProjectSearchRes> result = searchService.searchProjects(request);
        return ResponseEntity.ok(result);
    }

    // 프로젝트 테스트용 엔드포인트
    @GetMapping("/test/projects/count")
    public ResponseEntity<Map<String, Object>> testProjectCount() {
        long count = searchService.countAllProjects();
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    @GetMapping("/test/projects/all")
    public ResponseEntity<Map<String, Object>> testFindAllProjects() {
        Iterable<ProjectSearchDocument> projects = searchService.findAllProjects();
        List<ProjectSearchDocument> projectList = new ArrayList<>();
        projects.forEach(projectList::add);
        return ResponseEntity.ok(Map.of("projects", projectList, "size", projectList.size()));
    }
    
}
