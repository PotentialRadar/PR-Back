package com.potential_radar.PR.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchAnalyzerConfig {

    private final ElasticsearchOperations elasticsearchOperations;
    
    @PostConstruct
    public void setupAnalyzers() {
        log.info("Elasticsearch analyzer configuration loaded. Custom analyzers will be applied during indexing.");
        // 실제 인덱스 설정은 Document 어노테이션에서 처리됩니다.
        // 복잡한 설정은 elasticsearch.yml 또는 REST API로 처리하는 것이 더 안정적입니다.
    }
}