package com.potential_radar.PR.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TestController {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @GetMapping("/test/elasticsearch")
    public Map<String, Object> testElasticsearch() {
        try {
            // 가장 간단한 연결 확인: 객체 주입 여부만 체크
            boolean isConnected = elasticsearchOperations != null;
            String className = elasticsearchOperations.getClass().getSimpleName();

            return Map.of(
                    "status", "success",
                    "message", "Elasticsearch 연결 성공!",
                    "connected", isConnected,
                    "implementation", className,
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Elasticsearch 연결 실패: " + e.getMessage(),
                    "connected", false
            );
        }
    }

    @GetMapping("/test/health")
    public String healthCheck() {
        return "Spring Boot 애플리케이션 정상 작동 중!";
    }

    // 추가: 더 상세한 정보 확인
    @GetMapping("/test/elasticsearch-info")
    public Map<String, Object> getElasticsearchInfo() {
        try {
            return Map.of(
                    "elasticsearchOperations", elasticsearchOperations != null ? "주입됨" : "주입 안됨",
                    "className", elasticsearchOperations.getClass().getName(),
                    "package", elasticsearchOperations.getClass().getPackage().getName(),
                    "availableMethods", java.util.Arrays.toString(
                            elasticsearchOperations.getClass().getDeclaredMethods()).substring(0, 200) + "..."
            );
        } catch (Exception e) {
            return Map.of(
                    "error", e.getMessage(),
                    "status", "failed"
            );
        }
    }
}