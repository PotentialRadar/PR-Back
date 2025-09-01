package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.search.service.PopularSearchService;
import com.potential_radar.PR.search.service.SearchCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/popular")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PopularSearchController {
    
    private final PopularSearchService popularSearchService;
    private final SearchCacheService searchCacheService;
    
    @GetMapping("/keywords")
    public ResponseEntity<Map<String, Object>> getPopularKeywords() {
        List<String> keywords = popularSearchService.getPopularKeywords();
        return ResponseEntity.ok(Map.of(
            "keywords", keywords,
            "count", keywords.size()
        ));
    }
    
    @GetMapping("/tech-stacks")
    public ResponseEntity<Map<String, Object>> getPopularTechStacks() {
        List<String> techStacks = popularSearchService.getPopularTechStacks();
        return ResponseEntity.ok(Map.of(
            "techStacks", techStacks,
            "count", techStacks.size()
        ));
    }
    
    @GetMapping("/tech-parts")
    public ResponseEntity<Map<String, Object>> getPopularTechParts() {
        List<String> techParts = popularSearchService.getPopularTechParts();
        return ResponseEntity.ok(Map.of(
            "techParts", techParts,
            "count", techParts.size()
        ));
    }
    
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllPopularItems() {
        return ResponseEntity.ok(Map.of(
            "keywords", popularSearchService.getPopularKeywords(),
            "techStacks", popularSearchService.getPopularTechStacks(),
            "techParts", popularSearchService.getPopularTechParts()
        ));
    }
    
    @PostMapping("/update")
    public ResponseEntity<Map<String, String>> forceUpdate() {
        try {
            popularSearchService.updatePopularItems();
            return ResponseEntity.ok(Map.of(
                "message", "Popular search items updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to update popular items: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearSearchCache() {
        try {
            searchCacheService.clearAllSearchCache();
            return ResponseEntity.ok(Map.of(
                "message", "Search cache cleared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to clear cache: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/cache/evict/{pattern}")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable String pattern) {
        try {
            searchCacheService.evictProjectSearchCache(pattern);
            return ResponseEntity.ok(Map.of(
                "message", "Cache evicted for pattern: " + pattern
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to evict cache: " + e.getMessage()
            ));
        }
    }
}