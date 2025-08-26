package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.dto.*;
import com.potential_radar.PR.search.service.SearchService;
import com.potential_radar.PR.search.service.DataSyncService;
import com.potential_radar.PR.search.service.PopularSearchService;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SearchController {

    private final SearchService searchService;
    private final DataSyncService dataSyncService;
    private final PopularSearchService popularSearchService;
    private final UserSearchRepository userSearchRepository;
    private final ProjectSearchRepository projectSearchRepository;

    /**
     * String 리스트를 ExperienceRange로 안전하게 변환
     */
    private List<ExperienceRange> parseExperienceRanges(List<String> experienceRanges) {
        if (experienceRanges == null || experienceRanges.isEmpty()) {
            return null;
        }
        
        try {
            return experienceRanges.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(s -> s.trim().toUpperCase())
                    .map(ExperienceRange::valueOf)
                    .collect(java.util.stream.Collectors.toList());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid experienceRanges value: " + experienceRanges, ex);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<SearchResult<UserSearchRes>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String nickname, // nickname 파라미터 추가
            @RequestParam(required = false) List<String> techParts,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) List<String> experienceRanges,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Received search request - experienceRanges: {}", experienceRanges);
        
        // nickname 파라미터가 있으면 keyword로 사용
        String searchKeyword = (nickname != null && !nickname.trim().isEmpty()) ? nickname : keyword;
        
        // String을 ExperienceRange로 안전하게 변환
        List<ExperienceRange> experienceEnums = parseExperienceRanges(experienceRanges);

        UserSearchReq request = UserSearchReq.builder()
                .keyword(searchKeyword)
                .techParts(techParts)
                .techStacks(techStacks)
                .experienceRanges(experienceEnums)
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

    // 기술 태그 조회 엔드포인트
    @GetMapping("/tags")
    public ResponseEntity<TechTagsRes> getTechTags() {
        TechTagsRes techTags = searchService.getTechTags();
        return ResponseEntity.ok(techTags);
    }

    // 프로젝트 필터별 결과 수 미리보기 엔드포인트
    @GetMapping("/projects/count-preview")
    public ResponseEntity<Map<String, Object>> getProjectCountPreview(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> techParts,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) List<String> statuses) {

        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword(keyword)
                .techParts(techParts)
                .techStacks(techStacks)
                .statuses(statuses)
                .page(0)
                .size(1) // 결과 수만 필요하므로 최소 size
                .build();

        SearchResult<ProjectSearchRes> result = searchService.searchProjects(request);
        
        return ResponseEntity.ok(Map.of(
                "totalCount", result.getTotalElements(),
                "searchTime", result.getSearchTimeMs()
        ));
    }

    // 사용자(포트폴리오) 필터별 결과 수 미리보기 엔드포인트
    @GetMapping("/users/count-preview")
    public ResponseEntity<Map<String, Object>> getUserCountPreview(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) List<String> techParts,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) List<String> experienceRanges) {

        // nickname 파라미터가 있으면 keyword로 사용
        String searchKeyword = (nickname != null && !nickname.trim().isEmpty()) ? nickname : keyword;
        
        // String을 ExperienceRange로 안전하게 변환
        List<ExperienceRange> experienceEnums = parseExperienceRanges(experienceRanges);

        UserSearchReq request = UserSearchReq.builder()
                .keyword(searchKeyword)
                .techParts(techParts)
                .techStacks(techStacks)
                .experienceRanges(experienceEnums)
                .page(0)
                .size(1) // 결과 수만 필요하므로 최소 size
                .build();

        SearchResult<UserSearchRes> result = searchService.searchUsers(request);
        
        return ResponseEntity.ok(Map.of(
                "totalCount", result.getTotalElements(),
                "searchTime", result.getSearchTimeMs()
        ));
    }

    // 포트폴리오 전용 인기 키워드 조회 엔드포인트
    @GetMapping("/popular/user-keywords")
    public ResponseEntity<Map<String, Object>> getPopularUserKeywords() {
        try {
            List<String> keywords = popularSearchService.getPopularUserKeywords();
            return ResponseEntity.ok(Map.of("keywords", keywords));
        } catch (Exception e) {
            log.error("Failed to get popular user keywords: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get popular user keywords"));
        }
    }

    // 데이터 동기화 엔드포인트
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncData() {
        try {
            dataSyncService.syncAllData();
            return ResponseEntity.ok(Map.of("message", "Data synchronized successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to sync data: " + e.getMessage()));
        }
    }


    // 인덱스 재생성 엔드포인트 (개발용)
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, String>> reindexData() {
        try {
            // 기존 인덱스 삭제
            projectSearchRepository.deleteAll();
            userSearchRepository.deleteAll();
            
            // 데이터 재동기화
            dataSyncService.syncAllData();
            
            return ResponseEntity.ok(Map.of("message", "Reindexing completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reindex: " + e.getMessage()));
        }
    }
    
}
