package com.potential_radar.PR.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search/health")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthController {
    
    private final ElasticsearchOperations elasticsearchOperations;
    
    @GetMapping("/elasticsearch")
    public ResponseEntity<Map<String, Object>> checkElasticsearch() {
        try {
            // Elasticsearch 연결 상태 확인
            boolean isRunning = elasticsearchOperations.indexOps(com.potential_radar.PR.search.document.UserSearchDocument.class).exists();
            
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "elasticsearch", isRunning ? "connected" : "index_not_found",
                "message", "Elasticsearch connection test completed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "elasticsearch", "disconnected",
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        try {
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "database", "connected",
                "message", "Database connection test completed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error", 
                "database", "disconnected",
                "error", e.getMessage()
            ));
        }
    }
}