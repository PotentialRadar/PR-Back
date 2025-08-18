package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.dto.*;
import com.potential_radar.PR.search.document.SearchHistoryDocument;
import com.potential_radar.PR.search.service.SearchService;
import com.potential_radar.PR.search.service.AutoCompleteService;
import com.potential_radar.PR.search.service.SearchHistoryService;
import com.potential_radar.PR.search.service.DataSyncService;
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
    private final AutoCompleteService autoCompleteService;
    private final SearchHistoryService searchHistoryService;
    private final DataSyncService dataSyncService;
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

    @GetMapping("/projects")
    public ResponseEntity<SearchResult<ProjectSearchRes>> searchProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> techParts,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword(keyword)
                .techParts(techParts)
                .techStacks(techStacks)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        SearchResult<ProjectSearchRes> result = searchService.searchProjects(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unified")
    public ResponseEntity<UnifiedSearchRes> unifiedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String searchType,
            @RequestParam(required = false) List<String> techStacks,
            @RequestParam(required = false) String techPart,
            @RequestParam(required = false) List<String> requiredTechParts,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UnifiedSearchReq request = UnifiedSearchReq.builder()
                .keyword(keyword)
                .searchType(searchType)
                .techStacks(techStacks)
                .techPart(techPart)
                .requiredTechParts(requiredTechParts)
                .page(page)
                .size(size)
                .build();

        UnifiedSearchRes result = searchService.unifiedSearch(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<AutoCompleteRes> getAutoComplete(
            @RequestParam String query,
            @RequestParam(defaultValue = "all") String type) {
        
        AutoCompleteRes result = autoCompleteService.getAutoCompleteSuggestions(query, type);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<SearchHistoryDocument>> getSearchHistory(
            @PathVariable Long userId) {
        
        List<SearchHistoryDocument> history = searchHistoryService.getRecentSearchHistory(userId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history/{userId}")
    public ResponseEntity<Void> deleteSearchHistory(@PathVariable Long userId) {
        searchHistoryService.deleteUserSearchHistory(userId);
        return ResponseEntity.ok().build();
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
    
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncData() {
        try {
            dataSyncService.syncAllData();
            return ResponseEntity.ok(Map.of("message", "Data synchronized successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/test/nickname/{nickname}")
    public ResponseEntity<Map<String, Object>> testNicknameSearch(@PathVariable String nickname) {
        try {
            Iterable<UserSearchDocument> users = userSearchRepository.findByNickname(nickname);
            List<UserSearchDocument> userList = new ArrayList<>();
            users.forEach(userList::add);
            return ResponseEntity.ok(Map.of(
                "searchNickname", nickname,
                "foundUsers", userList,
                "count", userList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/test/techpart/{techPart}")
    public ResponseEntity<Map<String, Object>> testTechPartSearch(@PathVariable String techPart) {
        try {
            Iterable<UserSearchDocument> users = userSearchRepository.findByTechPart(techPart);
            List<UserSearchDocument> userList = new ArrayList<>();
            users.forEach(userList::add);
            return ResponseEntity.ok(Map.of(
                "searchTechPart", techPart,
                "foundUsers", userList,
                "count", userList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/test/techstack/{techStack}")
    public ResponseEntity<Map<String, Object>> testTechStackSearch(@PathVariable String techStack) {
        try {
            Iterable<UserSearchDocument> users = userSearchRepository.findByTechStacksContaining(techStack);
            List<UserSearchDocument> userList = new ArrayList<>();
            users.forEach(userList::add);
            return ResponseEntity.ok(Map.of(
                "searchTechStack", techStack,
                "foundUsers", userList,
                "count", userList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/test/nicknamecontains/{nickname}")
    public ResponseEntity<Map<String, Object>> testNicknameContainsSearch(@PathVariable String nickname) {
        try {
            Iterable<UserSearchDocument> users = userSearchRepository.findByNicknameContaining(nickname);
            List<UserSearchDocument> userList = new ArrayList<>();
            users.forEach(userList::add);
            return ResponseEntity.ok(Map.of(
                "searchNickname", nickname,
                "foundUsers", userList,
                "count", userList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/test/experience/{experienceRange}")
    public ResponseEntity<Map<String, Object>> testExperienceSearch(@PathVariable String experienceRange) {
        try {
            Iterable<UserSearchDocument> users = userSearchRepository.findByExperienceRange(experienceRange);
            List<UserSearchDocument> userList = new ArrayList<>();
            users.forEach(userList::add);
            return ResponseEntity.ok(Map.of(
                "searchExperience", experienceRange,
                "foundUsers", userList,
                "count", userList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/tags")
    public ResponseEntity<TechTagsRes> getTechTags() {
        try {
            TechTagsRes tags = searchService.getTechTags();
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
