package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.repository.SearchEventRepository;
import com.potential_radar.PR.project.repository.ProjectTechStackRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularSearchServiceTest {

    @Mock
    private SearchEventRepository searchEventRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ProjectTechStackRepository projectTechStackRepository;
    
    @Mock
    private UserTechStackRepository userTechStackRepository;
    
    @InjectMocks
    private PopularSearchService popularSearchService;
    
    @BeforeEach
    void setUp() {
        // ReflectionTestUtils를 사용하여 private 필드 설정
        ReflectionTestUtils.setField(popularSearchService, "popularSearchDays", 30);
        ReflectionTestUtils.setField(popularSearchService, "minSearchCount", 5);
        ReflectionTestUtils.setField(popularSearchService, "cacheDurationHours", 168); // 1주일
        ReflectionTestUtils.setField(popularSearchService, "maxKeywords", 20);
        ReflectionTestUtils.setField(popularSearchService, "maxTechStacks", 15);
        ReflectionTestUtils.setField(popularSearchService, "maxTechParts", 10);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void updatePopularKeywords_최소검색횟수이상_캐시저장() {
        // Given
        Object[] result1 = {"Spring", 10L};
        Object[] result2 = {"React", 8L};
        Object[] result3 = {"Vue", 3L}; // 최소 검색 횟수 미만
        
        List<Object[]> mockResults = Arrays.asList(result1, result2, result3);
        
        when(searchEventRepository.findPopularKeywords(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(mockResults);
        
        // When
        ReflectionTestUtils.invokeMethod(popularSearchService, "updatePopularKeywords");
        
        // Then
        verify(valueOperations).set(
                eq("popular:keywords"), 
                argThat(keywords -> {
                    List<String> keywordList = (List<String>) keywords;
                    return keywordList.size() == 2 && 
                           keywordList.contains("Spring") && 
                           keywordList.contains("React") &&
                           !keywordList.contains("Vue"); // 최소 검색 횟수 미만이므로 제외
                }), 
                eq(Duration.ofHours(168))
        );
    }
    
    @Test
    void updatePopularTechStacks_프로젝트기반_캐시저장() {
        // Given
        Object[] result1 = {"Spring Boot", 25L};
        Object[] result2 = {"MySQL", 20L};
        Object[] result3 = {"Redis", 15L};
        
        List<Object[]> mockResults = Arrays.asList(result1, result2, result3);
        
        when(projectTechStackRepository.findMostUsedTechStacks())
                .thenReturn(mockResults);
        
        // When
        ReflectionTestUtils.invokeMethod(popularSearchService, "updatePopularTechStacks");
        
        // Then
        verify(valueOperations).set(
                eq("popular:techstacks"), 
                argThat(techStacks -> {
                    List<String> techStackList = (List<String>) techStacks;
                    return techStackList.size() == 3 && 
                           techStackList.contains("Spring Boot") && 
                           techStackList.contains("MySQL") &&
                           techStackList.contains("Redis");
                }), 
                eq(Duration.ofHours(168))
        );
    }
    
    @Test
    void updatePopularTechParts_최소검색횟수필터링_캐시저장() {
        // Given
        Object[] result1 = {"백엔드", 12L};
        Object[] result2 = {"프론트엔드", 8L};
        Object[] result3 = {"모바일", 3L}; // 최소 검색 횟수 미만
        
        List<Object[]> mockResults = Arrays.asList(result1, result2, result3);
        
        when(searchEventRepository.findPopularTechParts(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(mockResults);
        
        // When
        ReflectionTestUtils.invokeMethod(popularSearchService, "updatePopularTechParts");
        
        // Then
        verify(valueOperations).set(
                eq("popular:techparts"), 
                argThat(techParts -> {
                    List<String> techPartList = (List<String>) techParts;
                    return techPartList.size() == 2 && 
                           techPartList.contains("백엔드") && 
                           techPartList.contains("프론트엔드") &&
                           !techPartList.contains("모바일"); // 최소 검색 횟수 미만이므로 제외
                }), 
                eq(Duration.ofHours(168))
        );
    }
    
    @Test
    void getPopularKeywords_캐시있음_반환() {
        // Given
        List<String> cachedKeywords = Arrays.asList("Spring", "React", "Java");
        when(valueOperations.get("popular:keywords")).thenReturn(cachedKeywords);
        
        // When
        List<String> result = popularSearchService.getPopularKeywords();
        
        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("Spring"));
        assertTrue(result.contains("React"));
        assertTrue(result.contains("Java"));
    }
    
    @Test
    void getPopularKeywords_캐시없음_빈리스트반환() {
        // Given
        when(valueOperations.get("popular:keywords")).thenReturn(null);
        
        // When
        List<String> result = popularSearchService.getPopularKeywords();
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getPopularTechStacks_캐시있음_반환() {
        // Given
        List<String> cachedTechStacks = Arrays.asList("Spring Boot", "React", "MySQL");
        when(valueOperations.get("popular:techstacks")).thenReturn(cachedTechStacks);
        
        // When
        List<String> result = popularSearchService.getPopularTechStacks();
        
        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("Spring Boot"));
        assertTrue(result.contains("React"));
        assertTrue(result.contains("MySQL"));
    }
    
    @Test
    void getPopularUserKeywords_사용자전용캐시_반환() {
        // Given
        List<String> cachedUserKeywords = Arrays.asList("Java", "Python", "JavaScript");
        when(valueOperations.get("popular:user:keywords")).thenReturn(cachedUserKeywords);
        
        // When
        List<String> result = popularSearchService.getPopularUserKeywords();
        
        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("Java"));
        assertTrue(result.contains("Python"));
        assertTrue(result.contains("JavaScript"));
    }
    
    @Test
    void updatePopularUserTechStacks_사용자기반_캐시저장() {
        // Given
        Object[] result1 = {"Java", 30L};
        Object[] result2 = {"Python", 25L};
        Object[] result3 = {"JavaScript", 20L};
        
        List<Object[]> mockResults = Arrays.asList(result1, result2, result3);
        
        when(userTechStackRepository.findMostUsedTechStacks())
                .thenReturn(mockResults);
        
        // When
        ReflectionTestUtils.invokeMethod(popularSearchService, "updatePopularUserTechStacks");
        
        // Then
        verify(valueOperations).set(
                eq("popular:user:techstacks"), 
                argThat(techStacks -> {
                    List<String> techStackList = (List<String>) techStacks;
                    return techStackList.size() == 3 && 
                           techStackList.contains("Java") && 
                           techStackList.contains("Python") &&
                           techStackList.contains("JavaScript");
                }), 
                eq(Duration.ofHours(168))
        );
    }
    
    @Test
    void updatePopularItems_전체업데이트_모든메서드호출() {
        // Given
        when(searchEventRepository.findPopularKeywords(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(searchEventRepository.findPopularTechParts(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(searchEventRepository.findPopularUserKeywords(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(searchEventRepository.findPopularUserTechParts(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(projectTechStackRepository.findMostUsedTechStacks())
                .thenReturn(Collections.emptyList());
        when(userTechStackRepository.findMostUsedTechStacks())
                .thenReturn(Collections.emptyList());
        
        // When
        popularSearchService.updatePopularItems();
        
        // Then
        // 모든 캐시 키에 대해 저장이 호출되었는지 확인
        verify(valueOperations, times(6)).set(anyString(), any(), any(Duration.class));
    }
}