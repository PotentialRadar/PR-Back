package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.ProjectSearchRes;
import com.potential_radar.PR.search.dto.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 캐시 검증 테스트
 * 1. 인기 검색어 1주일 캐싱 확인
 * 2. 검색 결과 5분 캐싱 확인  
 * 3. 캐시 TTL 동작 확인
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheValidationTest {

    @Autowired
    private PopularSearchService popularSearchService;

    @Autowired
    private SmartCacheService smartCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void 인기검색어_일주일캐싱_확인() throws InterruptedException {
        // Given
        System.out.println("=== 인기 검색어 캐시 테스트 시작 ===");
        
        // 기존 캐시 클리어
        redisTemplate.delete("popular:keywords");
        redisTemplate.delete("popular:techstacks");
        redisTemplate.delete("popular:techparts");

        // 인기 검색어 수동 업데이트 (실제로는 스케줄러가 실행)
        popularSearchService.updatePopularItems();
        
        // When
        List<String> keywords1 = popularSearchService.getPopularKeywords();
        List<String> techStacks1 = popularSearchService.getPopularTechStacks();
        List<String> techParts1 = popularSearchService.getPopularTechParts();
        
        System.out.println("인기 검색어: " + keywords1);
        System.out.println("인기 기술스택: " + techStacks1);  
        System.out.println("인기 기술파트: " + techParts1);
        
        // TTL 확인 (168시간 = 604800초)
        Long keywordTtl = redisTemplate.getExpire("popular:keywords", TimeUnit.SECONDS);
        Long techStackTtl = redisTemplate.getExpire("popular:techstacks", TimeUnit.SECONDS);
        Long techPartTtl = redisTemplate.getExpire("popular:techparts", TimeUnit.SECONDS);
        
        System.out.println("Keywords TTL: " + keywordTtl + "초 (약 " + (keywordTtl/3600) + "시간)");
        System.out.println("TechStacks TTL: " + techStackTtl + "초 (약 " + (techStackTtl/3600) + "시간)");
        System.out.println("TechParts TTL: " + techPartTtl + "초 (약 " + (techPartTtl/3600) + "시간)");
        
        // 짧은 대기 후 재조회 (캐시에서 읽어야 함)
        Thread.sleep(1000);
        
        List<String> keywords2 = popularSearchService.getPopularKeywords();
        List<String> techStacks2 = popularSearchService.getPopularTechStacks();
        
        // Then
        assertEquals(keywords1, keywords2, "캐시된 인기 검색어가 일관되게 반환되어야 함");
        assertEquals(techStacks1, techStacks2, "캐시된 인기 기술스택이 일관되게 반환되어야 함");
        
        // TTL이 대략 1주일(604800초) 근처여야 함 (허용 오차: ±100초)
        assertTrue(keywordTtl > 604700 && keywordTtl <= 604800, 
            "Keywords TTL이 1주일 범위에 있어야 함: " + keywordTtl);
        assertTrue(techStackTtl > 604700 && techStackTtl <= 604800, 
            "TechStacks TTL이 1주일 범위에 있어야 함: " + techStackTtl);
        
        System.out.println("✅ 인기 검색어 1주일 캐싱 검증 완료");
    }

    @Test
    void 인기검색어결과_5분캐싱_확인() throws InterruptedException {
        // Given
        System.out.println("=== 검색 결과 5분 캐싱 테스트 시작 ===");
        
        // 먼저 인기 검색어 설정
        popularSearchService.updatePopularItems();
        List<String> popularKeywords = popularSearchService.getPopularKeywords();
        
        if (popularKeywords.isEmpty()) {
            System.out.println("⚠️ 인기 검색어가 없어서 테스트용 검색어로 진행");
            // 테스트용으로 직접 Redis에 인기 검색어 추가
            redisTemplate.opsForValue().set("popular:keywords", 
                Arrays.asList("Spring", "React", "Java"), 
                java.time.Duration.ofHours(168));
            popularKeywords = Arrays.asList("Spring", "React", "Java");
        }
        
        // 인기 검색어 중 첫 번째 선택
        String testKeyword = popularKeywords.get(0);
        System.out.println("테스트 키워드: " + testKeyword);
        
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword(testKeyword)
                .page(0)
                .size(10)
                .sortBy("latest")
                .build();

        // 기존 검색 결과 캐시 클리어
        smartCacheService.evictProjectSearchCache();
        
        // When
        // 1. 인기 검색인지 확인
        boolean isPopular = smartCacheService.shouldCacheProjectSearch(request);
        System.out.println("인기 검색 여부: " + isPopular);
        assertTrue(isPopular, "테스트 키워드가 인기 검색어로 인식되어야 함");
        
        // 2. 검색 결과 생성 및 캐싱
        SearchResult<ProjectSearchRes> testResult = SearchResult.<ProjectSearchRes>builder()
                .content(Collections.emptyList())
                .totalElements(10L)
                .totalPages(1)
                .page(0)
                .size(10)
                .hasNext(false)
                .hasPrevious(false)
                .searchTimeMs(100L)
                .fromCache(false)
                .build();
        
        // 캐시에 저장
        smartCacheService.cacheProjectSearchResult(request, testResult);
        System.out.println("검색 결과를 캐시에 저장");
        
        // 3. 캐시에서 조회
        SearchResult<ProjectSearchRes> cachedResult = smartCacheService.getCachedProjectSearchResult(request);
        assertNotNull(cachedResult, "캐시된 결과가 조회되어야 함");
        assertEquals(testResult.getTotalElements(), cachedResult.getTotalElements());
        System.out.println("캐시에서 조회 성공: " + cachedResult.getTotalElements() + "개 결과");
        
        // 4. TTL 확인 (5분 = 300초)
        String cacheKey = smartCacheService.generateProjectCacheKey(request);
        Long searchResultTtl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        System.out.println("검색 결과 TTL: " + searchResultTtl + "초 (약 " + (searchResultTtl/60.0) + "분)");
        
        // TTL이 대략 5분(300초) 근처여야 함 (허용 오차: ±10초)
        assertTrue(searchResultTtl > 290 && searchResultTtl <= 300, 
            "검색 결과 TTL이 5분 범위에 있어야 함: " + searchResultTtl);
        
        System.out.println("✅ 검색 결과 5분 캐싱 검증 완료");
    }

    @Test
    void 캐시무효화_동작확인() {
        // Given
        System.out.println("=== 캐시 무효화 테스트 시작 ===");
        
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("TestKeyword")
                .build();

        SearchResult<ProjectSearchRes> testResult = SearchResult.<ProjectSearchRes>builder()
                .content(Collections.emptyList())
                .totalElements(5L)
                .build();

        // 캐시에 저장
        smartCacheService.cacheProjectSearchResult(request, testResult);
        
        // 캐시 존재 확인
        SearchResult<ProjectSearchRes> beforeEviction = smartCacheService.getCachedProjectSearchResult(request);
        assertNotNull(beforeEviction, "캐시가 존재해야 함");
        System.out.println("캐시 저장 확인: " + beforeEviction.getTotalElements() + "개 결과");

        // When - 캐시 무효화
        smartCacheService.evictProjectSearchCache();
        System.out.println("검색 결과 캐시 무효화 실행");

        // Then - 캐시 삭제 확인
        SearchResult<ProjectSearchRes> afterEviction = smartCacheService.getCachedProjectSearchResult(request);
        assertNull(afterEviction, "캐시 무효화 후 null이 반환되어야 함");
        System.out.println("✅ 캐시 무효화 검증 완료");
    }

    @Test  
    void 인기데이터_vs_검색결과_TTL차이_확인() {
        // Given
        System.out.println("=== 캐시 TTL 차이 검증 테스트 ===");
        
        // 인기 데이터 업데이트
        popularSearchService.updatePopularItems();
        
        // 검색 결과 캐싱
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();
        
        SearchResult<ProjectSearchRes> testResult = SearchResult.<ProjectSearchRes>builder()
                .content(Collections.emptyList())
                .totalElements(3L)
                .build();
        
        smartCacheService.cacheProjectSearchResult(request, testResult);
        
        // When - TTL 조회
        Long popularKeywordsTtl = redisTemplate.getExpire("popular:keywords", TimeUnit.SECONDS);
        String searchCacheKey = smartCacheService.generateProjectCacheKey(request);
        Long searchResultsTtl = redisTemplate.getExpire(searchCacheKey, TimeUnit.SECONDS);
        
        System.out.println("인기 검색어 TTL: " + popularKeywordsTtl + "초 (약 " + (popularKeywordsTtl/3600.0) + "시간)");
        System.out.println("검색 결과 TTL: " + searchResultsTtl + "초 (약 " + (searchResultsTtl/60.0) + "분)");
        
        // Then - TTL 차이 검증
        // 인기 데이터: 약 1주일 (604800초)
        // 검색 결과: 약 5분 (300초)
        assertTrue(popularKeywordsTtl > 600000, "인기 검색어는 1주일 가까이 캐시되어야 함");
        assertTrue(searchResultsTtl < 400, "검색 결과는 5분 가까이 캐시되어야 함");
        assertTrue(popularKeywordsTtl > searchResultsTtl * 1000, "인기 데이터가 검색 결과보다 훨씬 오래 캐시되어야 함");
        
        System.out.println("✅ TTL 차이 검증 완료");
        System.out.println("인기 데이터는 검색 결과보다 " + (popularKeywordsTtl/searchResultsTtl) + "배 오래 캐시됨");
    }
}