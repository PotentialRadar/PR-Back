package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.ProjectSearchRes;
import com.potential_radar.PR.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 스마트 캐시 서비스 통합 테스트
 * 실제 Redis와 연동하여 캐시 동작을 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class SmartCacheServiceIntegrationTest {

    @Autowired
    private SmartCacheService smartCacheService;

    @Autowired
    private PopularSearchService popularSearchService;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 캐시 클리어
        smartCacheService.evictAllSearchResultCache();
    }

    @Test
    void 인기검색어_캐싱_통합테스트() {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .page(0)
                .size(10)
                .sortBy("latest")
                .build();

        SearchResult<ProjectSearchRes> originalResult = SearchResult.<ProjectSearchRes>builder()
                .content(Collections.emptyList())
                .totalElements(5L)
                .totalPages(1)
                .page(0)
                .size(10)
                .hasNext(false)
                .hasPrevious(false)
                .searchTimeMs(100L)
                .build();

        // When - 캐시에 저장
        smartCacheService.cacheProjectSearchResult(request, originalResult);

        // Then - 캐시에서 조회
        SearchResult<ProjectSearchRes> cachedResult = smartCacheService.getCachedProjectSearchResult(request);

        assertNotNull(cachedResult);
        assertEquals(originalResult.getTotalElements(), cachedResult.getTotalElements());
        assertEquals(originalResult.getTotalPages(), cachedResult.getTotalPages());
        assertEquals(originalResult.getPage(), cachedResult.getPage());
        assertEquals(originalResult.getSize(), cachedResult.getSize());
    }

    @Test
    void 캐시키_생성_일관성_테스트() {
        // Given
        ProjectSearchReq request1 = ProjectSearchReq.builder()
                .keyword("Spring")
                .techStacks(Arrays.asList("Spring Boot", "MySQL"))
                .page(0)
                .size(10)
                .build();

        ProjectSearchReq request2 = ProjectSearchReq.builder()
                .keyword("Spring")
                .techStacks(Arrays.asList("Spring Boot", "MySQL"))
                .page(0)
                .size(10)
                .build();

        // When
        String key1 = smartCacheService.generateProjectCacheKey(request1);
        String key2 = smartCacheService.generateProjectCacheKey(request2);

        // Then
        assertEquals(key1, key2, "동일한 요청에 대해 같은 캐시 키가 생성되어야 함");
    }

    @Test
    void 캐시키_생성_차이점_테스트() {
        // Given
        ProjectSearchReq request1 = ProjectSearchReq.builder()
                .keyword("Spring")
                .page(0)
                .size(10)
                .build();

        ProjectSearchReq request2 = ProjectSearchReq.builder()
                .keyword("React")
                .page(0)
                .size(10)
                .build();

        // When
        String key1 = smartCacheService.generateProjectCacheKey(request1);
        String key2 = smartCacheService.generateProjectCacheKey(request2);

        // Then
        assertNotEquals(key1, key2, "다른 요청에 대해 다른 캐시 키가 생성되어야 함");
    }

    @Test
    void 캐시_무효화_테스트() {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();

        SearchResult<ProjectSearchRes> result = SearchResult.<ProjectSearchRes>builder()
                .content(Collections.emptyList())
                .totalElements(3L)
                .build();

        // When - 캐시 저장
        smartCacheService.cacheProjectSearchResult(request, result);
        
        // 캐시가 있는지 확인
        SearchResult<ProjectSearchRes> cachedResult = smartCacheService.getCachedProjectSearchResult(request);
        assertNotNull(cachedResult, "캐시가 저장되어 있어야 함");

        // 캐시 무효화
        smartCacheService.evictProjectSearchCache();

        // Then - 캐시가 삭제되었는지 확인
        SearchResult<ProjectSearchRes> afterEviction = smartCacheService.getCachedProjectSearchResult(request);
        assertNull(afterEviction, "캐시 무효화 후 null이 반환되어야 함");
    }
}