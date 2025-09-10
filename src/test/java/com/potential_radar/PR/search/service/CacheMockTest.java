package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.ProjectSearchRes;
import com.potential_radar.PR.search.dto.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 모킹 데이터를 사용한 캐시 동작 검증 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheMockTest {

    @Autowired
    private SmartCacheService smartCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ValueOperations<String, Object> valueOps;

    @BeforeEach
    void setUp() {
        valueOps = redisTemplate.opsForValue();
        // 테스트 전 캐시 클리어
        redisTemplate.delete(redisTemplate.keys("*"));
    }

    @Test
    void 인기검색어_1주일_캐싱_검증() {
        System.out.println("=== 인기 검색어 1주일 캐싱 검증 ===");
        
        // Given - 테스트용 인기 검색어 Redis에 직접 설정 (1주일 TTL)
        Duration weekDuration = Duration.ofHours(168); // 1주일
        valueOps.set("popular:keywords", Arrays.asList("Spring", "React", "Java"), weekDuration);
        valueOps.set("popular:techstacks", Arrays.asList("Spring Boot", "MySQL", "Docker"), weekDuration);
        valueOps.set("popular:techparts", Arrays.asList("백엔드", "프론트엔드", "풀스택"), weekDuration);
        
        // When - TTL 확인
        Long keywordsTtl = redisTemplate.getExpire("popular:keywords", TimeUnit.SECONDS);
        Long techStacksTtl = redisTemplate.getExpire("popular:techstacks", TimeUnit.SECONDS);
        Long techPartsTtl = redisTemplate.getExpire("popular:techparts", TimeUnit.SECONDS);
        
        // Then - 1주일 = 604800초 (허용 오차 ±100초)
        System.out.println("Keywords TTL: " + keywordsTtl + "초 (약 " + (keywordsTtl/3600.0) + "시간)");
        System.out.println("TechStacks TTL: " + techStacksTtl + "초 (약 " + (techStacksTtl/3600.0) + "시간)");
        System.out.println("TechParts TTL: " + techPartsTtl + "초 (약 " + (techPartsTtl/3600.0) + "시간)");
        
        assertTrue(keywordsTtl > 604700 && keywordsTtl <= 604800, 
            "Keywords TTL이 1주일 범위에 있어야 함: " + keywordsTtl);
        assertTrue(techStacksTtl > 604700 && techStacksTtl <= 604800, 
            "TechStacks TTL이 1주일 범위에 있어야 함: " + techStacksTtl);
        assertTrue(techPartsTtl > 604700 && techPartsTtl <= 604800, 
            "TechParts TTL이 1주일 범위에 있어야 함: " + techPartsTtl);
        
        System.out.println("✅ 인기 검색어 1주일 캐싱 검증 완료");
    }

    @Test
    void 검색결과_5분_캐싱_검증() throws Exception {
        System.out.println("=== 검색 결과 5분 캐싱 검증 ===");
        
        // Given - 테스트용 인기 검색어 설정
        valueOps.set("popular:keywords", Arrays.asList("Spring", "React", "Java"), Duration.ofHours(168));
        
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")  // 인기 검색어
                .page(0)
                .size(10)
                .sortBy("latest")
                .build();

        SearchResult<ProjectSearchRes> testResult = SearchResult.<ProjectSearchRes>builder()
                .content(Collections.emptyList())
                .totalElements(15L)
                .totalPages(2)
                .page(0)
                .size(10)
                .hasNext(true)
                .hasPrevious(false)
                .searchTimeMs(120L)
                .fromCache(false)
                .build();

        // When - 검색 결과 캐싱
        smartCacheService.cacheProjectSearchResult(request, testResult);
        String cacheKey = smartCacheService.generateProjectCacheKey(request);
        
        // TTL 확인
        Long searchResultTtl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        
        // 캐시에서 조회
        SearchResult<ProjectSearchRes> cachedResult = smartCacheService.getCachedProjectSearchResult(request);
        
        // Then
        System.out.println("캐시 키: " + cacheKey);
        System.out.println("검색 결과 TTL: " + searchResultTtl + "초 (약 " + (searchResultTtl/60.0) + "분)");
        
        assertNotNull(cachedResult, "캐시된 결과가 조회되어야 함");
        assertEquals(testResult.getTotalElements(), cachedResult.getTotalElements());
        assertEquals(testResult.getTotalPages(), cachedResult.getTotalPages());
        
        // TTL이 5분(300초) 범위에 있어야 함 (허용 오차 ±10초)
        assertTrue(searchResultTtl > 290 && searchResultTtl <= 300, 
            "검색 결과 TTL이 5분 범위에 있어야 함: " + searchResultTtl);
        
        System.out.println("✅ 검색 결과 5분 캐싱 검증 완료");
    }

    @Test
    void 인기검색어_판단로직_검증() {
        System.out.println("=== 인기 검색어 판단 로직 검증 ===");
        
        // Given - 테스트용 인기 데이터 설정
        valueOps.set("popular:keywords", Arrays.asList("Spring", "React", "Java"), Duration.ofHours(168));
        valueOps.set("popular:techstacks", Arrays.asList("Spring Boot", "MySQL", "Docker"), Duration.ofHours(168));
        valueOps.set("popular:techparts", Arrays.asList("백엔드", "프론트엔드", "풀스택"), Duration.ofHours(168));
        
        // Test Case 1: 인기 키워드
        ProjectSearchReq popularKeywordReq = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();
        
        // Test Case 2: 인기 기술스택  
        ProjectSearchReq popularTechStackReq = ProjectSearchReq.builder()
                .techStacks(Arrays.asList("Spring Boot"))
                .build();
        
        // Test Case 3: 인기 기술파트
        ProjectSearchReq popularTechPartReq = ProjectSearchReq.builder()
                .techParts(Arrays.asList("백엔드"))
                .build();
        
        // Test Case 4: 비인기 검색
        ProjectSearchReq unpopularReq = ProjectSearchReq.builder()
                .keyword("UnknownTech")
                .build();
        
        // When & Then
        assertTrue(smartCacheService.shouldCacheProjectSearch(popularKeywordReq), 
            "인기 키워드는 캐시되어야 함");
        System.out.println("✅ 인기 키워드 'Spring' 캐싱 판단: true");
        
        assertTrue(smartCacheService.shouldCacheProjectSearch(popularTechStackReq), 
            "인기 기술스택은 캐시되어야 함");
        System.out.println("✅ 인기 기술스택 'Spring Boot' 캐싱 판단: true");
        
        assertTrue(smartCacheService.shouldCacheProjectSearch(popularTechPartReq), 
            "인기 기술파트는 캐시되어야 함");
        System.out.println("✅ 인기 기술파트 '백엔드' 캐싱 판단: true");
        
        assertFalse(smartCacheService.shouldCacheProjectSearch(unpopularReq), 
            "비인기 검색어는 캐시되지 않아야 함");
        System.out.println("✅ 비인기 검색어 'UnknownTech' 캐싱 판단: false");
        
        System.out.println("✅ 인기 검색어 판단 로직 검증 완료");
    }

    @Test
    void 캐시_무효화_동작_검증() throws Exception {
        System.out.println("=== 캐시 무효화 동작 검증 ===");
        
        // Given - 검색 결과 캐시 생성
        ProjectSearchReq request1 = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();
        
        ProjectSearchReq request2 = ProjectSearchReq.builder()
                .keyword("React")  
                .build();
        
        SearchResult<ProjectSearchRes> result1 = SearchResult.<ProjectSearchRes>builder()
                .totalElements(10L)
                .build();
        
        SearchResult<ProjectSearchRes> result2 = SearchResult.<ProjectSearchRes>builder()
                .totalElements(5L)
                .build();
        
        // 두 개의 검색 결과 캐시
        smartCacheService.cacheProjectSearchResult(request1, result1);
        smartCacheService.cacheProjectSearchResult(request2, result2);
        
        // 캐시 존재 확인
        assertNotNull(smartCacheService.getCachedProjectSearchResult(request1));
        assertNotNull(smartCacheService.getCachedProjectSearchResult(request2));
        System.out.println("✅ 두 개의 검색 결과 캐시 저장 확인");
        
        // When - 프로젝트 검색 캐시 무효화
        smartCacheService.evictProjectSearchCache();
        
        // Then - 캐시 삭제 확인
        assertNull(smartCacheService.getCachedProjectSearchResult(request1), 
            "첫 번째 캐시가 삭제되어야 함");
        assertNull(smartCacheService.getCachedProjectSearchResult(request2), 
            "두 번째 캐시가 삭제되어야 함");
        System.out.println("✅ 검색 결과 캐시 무효화 검증 완료");
    }

    @Test
    void TTL차이_검증() {
        System.out.println("=== 인기 데이터 vs 검색 결과 TTL 차이 검증 ===");
        
        // Given - 인기 데이터와 검색 결과 모두 캐시
        Duration weekDuration = Duration.ofHours(168); // 1주일  
        Duration fiveMinutes = Duration.ofMinutes(5);   // 5분
        
        // 인기 데이터 (1주일)
        valueOps.set("popular:keywords", Arrays.asList("Spring"), weekDuration);
        
        // 검색 결과 (5분)
        ProjectSearchReq request = ProjectSearchReq.builder().keyword("Spring").build();
        String searchCacheKey = smartCacheService.generateProjectCacheKey(request);
        valueOps.set(searchCacheKey, "{\"totalElements\":10}", fiveMinutes);
        
        // When - TTL 확인
        Long popularTtl = redisTemplate.getExpire("popular:keywords", TimeUnit.SECONDS);
        Long searchTtl = redisTemplate.getExpire(searchCacheKey, TimeUnit.SECONDS);
        
        // Then - TTL 비교
        System.out.println("인기 데이터 TTL: " + popularTtl + "초 (약 " + (popularTtl/3600.0) + "시간)");
        System.out.println("검색 결과 TTL: " + searchTtl + "초 (약 " + (searchTtl/60.0) + "분)");
        System.out.println("TTL 배율: " + (popularTtl.doubleValue()/searchTtl.doubleValue()) + "배");
        
        assertTrue(popularTtl > 600000, "인기 데이터는 1주일 가까이 캐시되어야 함");
        assertTrue(searchTtl < 400, "검색 결과는 5분 가까이 캐시되어야 함");
        assertTrue(popularTtl > searchTtl * 1000, "인기 데이터가 검색 결과보다 훨씬 오래 캐시되어야 함");
        
        System.out.println("✅ TTL 차이 검증 완료: 인기 데이터가 " + 
            String.format("%.1f", popularTtl.doubleValue()/searchTtl.doubleValue()) + "배 더 오래 캐시됨");
    }
}