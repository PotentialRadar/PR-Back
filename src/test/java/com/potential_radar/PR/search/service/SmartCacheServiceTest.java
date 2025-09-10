package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.ProjectSearchRes;
import com.potential_radar.PR.search.dto.UserSearchReq;
import com.potential_radar.PR.search.dto.UserSearchRes;
import com.potential_radar.PR.search.dto.SearchResult;
import com.potential_radar.PR.user.domain.ExperienceRange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private PopularSearchService popularSearchService;
    
    @InjectMocks
    private SmartCacheService smartCacheService;
    
    @BeforeEach
    void setUp() {
        // ReflectionTestUtils를 사용하여 private 필드 설정
        ReflectionTestUtils.setField(smartCacheService, "searchResultCacheMinutes", 5);
        ReflectionTestUtils.setField(smartCacheService, "popularityThreshold", 3);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void shouldCacheProjectSearch_인기키워드가있을때_true반환() {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();
        
        List<String> popularKeywords = Arrays.asList("Spring", "React", "Java");
        when(popularSearchService.getPopularKeywords()).thenReturn(popularKeywords);
        when(popularSearchService.getPopularTechStacks()).thenReturn(Arrays.asList());
        when(popularSearchService.getPopularTechParts()).thenReturn(Arrays.asList());
        
        // When
        boolean result = smartCacheService.shouldCacheProjectSearch(request);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldCacheProjectSearch_인기기술스택이있을때_true반환() {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .techStacks(Arrays.asList("Spring Boot", "MySQL"))
                .build();
        
        List<String> popularKeywords = Arrays.asList("Spring", "React", "Java");
        List<String> popularTechStacks = Arrays.asList("Spring Boot", "React", "Node.js");
        when(popularSearchService.getPopularKeywords()).thenReturn(popularKeywords);
        when(popularSearchService.getPopularTechStacks()).thenReturn(popularTechStacks);
        when(popularSearchService.getPopularTechParts()).thenReturn(Arrays.asList());
        
        // When
        boolean result = smartCacheService.shouldCacheProjectSearch(request);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldCacheProjectSearch_비인기검색어일때_false반환() {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("UnpopularTech")
                .build();
        
        List<String> popularKeywords = Arrays.asList("Spring", "React", "Java");
        when(popularSearchService.getPopularKeywords()).thenReturn(popularKeywords);
        when(popularSearchService.getPopularTechStacks()).thenReturn(Arrays.asList("Spring Boot", "MySQL"));
        when(popularSearchService.getPopularTechParts()).thenReturn(Arrays.asList("백엔드", "프론트엔드"));
        
        // When
        boolean result = smartCacheService.shouldCacheProjectSearch(request);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void shouldCacheUserSearch_인기사용자키워드가있을때_true반환() {
        // Given
        UserSearchReq request = UserSearchReq.builder()
                .keyword("Java")
                .build();
        
        List<String> popularUserKeywords = Arrays.asList("Java", "Python", "JavaScript");
        when(popularSearchService.getPopularUserKeywords()).thenReturn(popularUserKeywords);
        when(popularSearchService.getPopularUserTechStacks()).thenReturn(Arrays.asList());
        when(popularSearchService.getPopularUserTechParts()).thenReturn(Arrays.asList());
        
        // When
        boolean result = smartCacheService.shouldCacheUserSearch(request);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void generateProjectCacheKey_모든필터포함_올바른키생성() {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .techStacks(Arrays.asList("Spring Boot", "MySQL"))
                .techParts(Arrays.asList("백엔드"))
                .statuses(Arrays.asList("RECRUITING"))
                .page(0)
                .size(10)
                .sortBy("latest")
                .build();
        
        // When
        String cacheKey = smartCacheService.generateProjectCacheKey(request);
        
        // Then
        assertTrue(cacheKey.contains("kw:spring"));
        assertTrue(cacheKey.contains("ts:Spring Boot,MySQL"));
        assertTrue(cacheKey.contains("tp:백엔드"));
        assertTrue(cacheKey.contains("st:RECRUITING"));
        assertTrue(cacheKey.contains("page:0"));
        assertTrue(cacheKey.contains("size:10"));
        assertTrue(cacheKey.contains("sort:latest"));
    }
    
    @Test
    void generateUserCacheKey_경력범위포함_올바른키생성() {
        // Given
        UserSearchReq request = UserSearchReq.builder()
                .keyword("React")
                .techStacks(Arrays.asList("React", "TypeScript"))
                .techParts(Arrays.asList("프론트엔드"))
                .experienceRanges(Arrays.asList(ExperienceRange.Y1_3, ExperienceRange.Y3_5))
                .page(0)
                .size(20)
                .sortBy("popular")
                .build();
        
        // When
        String cacheKey = smartCacheService.generateUserCacheKey(request);
        
        // Then
        assertTrue(cacheKey.contains("kw:react"));
        assertTrue(cacheKey.contains("ts:React,TypeScript"));
        assertTrue(cacheKey.contains("tp:프론트엔드"));
        assertTrue(cacheKey.contains("exp:"));
        assertTrue(cacheKey.contains("page:0"));
        assertTrue(cacheKey.contains("size:20"));
        assertTrue(cacheKey.contains("sort:popular"));
    }
    
    @Test
    void getCachedProjectSearchResult_캐시가있을때_결과반환() throws Exception {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();
        
        SearchResult<ProjectSearchRes> expectedResult = SearchResult.<ProjectSearchRes>builder()
                .content(Arrays.asList())
                .totalElements(10L)
                .totalPages(1)
                .page(0)
                .size(10)
                .hasNext(false)
                .hasPrevious(false)
                .searchTimeMs(50L)
                .build();
        
        String cacheKey = smartCacheService.generateProjectCacheKey(request);
        String jsonResult = "{\"content\":[],\"totalElements\":10}";
        
        when(valueOperations.get(cacheKey)).thenReturn(jsonResult);
        when(objectMapper.readValue(eq(jsonResult), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(expectedResult);
        
        // When
        SearchResult<ProjectSearchRes> result = smartCacheService.getCachedProjectSearchResult(request);
        
        // Then
        assertNotNull(result);
        assertEquals(10L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }
    
    @Test
    void getCachedProjectSearchResult_캐시가없을때_null반환() {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();
        
        String cacheKey = smartCacheService.generateProjectCacheKey(request);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        
        // When
        SearchResult<ProjectSearchRes> result = smartCacheService.getCachedProjectSearchResult(request);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void cacheProjectSearchResult_정상_캐시저장() throws Exception {
        // Given
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")
                .build();
        
        SearchResult<ProjectSearchRes> searchResult = SearchResult.<ProjectSearchRes>builder()
                .content(Arrays.asList())
                .totalElements(5L)
                .build();
        
        String cacheKey = smartCacheService.generateProjectCacheKey(request);
        String jsonResult = "{\"content\":[],\"totalElements\":5}";
        
        when(objectMapper.writeValueAsString(searchResult)).thenReturn(jsonResult);
        
        // When
        smartCacheService.cacheProjectSearchResult(request, searchResult);
        
        // Then
        verify(valueOperations).set(eq(cacheKey), eq(jsonResult), eq(Duration.ofMinutes(5)));
    }
    
    @Test
    void cacheUserSearchResult_정상_캐시저장() throws Exception {
        // Given
        UserSearchReq request = UserSearchReq.builder()
                .keyword("Java")
                .build();
        
        SearchResult<UserSearchRes> searchResult = SearchResult.<UserSearchRes>builder()
                .content(Arrays.asList())
                .totalElements(3L)
                .build();
        
        String cacheKey = smartCacheService.generateUserCacheKey(request);
        String jsonResult = "{\"content\":[],\"totalElements\":3}";
        
        when(objectMapper.writeValueAsString(searchResult)).thenReturn(jsonResult);
        
        // When
        smartCacheService.cacheUserSearchResult(request, searchResult);
        
        // Then
        verify(valueOperations).set(eq(cacheKey), eq(jsonResult), eq(Duration.ofMinutes(5)));
    }
}