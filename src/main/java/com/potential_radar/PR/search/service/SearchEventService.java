package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.domain.SearchEvent;
import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.UserSearchReq;
import com.potential_radar.PR.search.repository.SearchEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchEventService {
    
    private final SearchEventRepository searchEventRepository;
    
    @Async
    @Transactional
    public void saveSearchLog(ProjectSearchReq request, long resultCount) {
        try {
            // 키워드 검색 로그
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                saveKeywordLog(request.getKeyword().trim(), resultCount);
            }
            
            // 단일 기술스택 검색 로그 (각각 개별 저장)
            if (request.getTechStacks() != null) {
                for (String techStack : request.getTechStacks()) {
                    saveTechStackLog(techStack, resultCount);
                }
            }
            
            // 단일 기술파트 검색 로그
            if (request.getTechParts() != null) {
                for (String techPart : request.getTechParts()) {
                    saveTechPartLog(techPart, resultCount);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to save search log: {}", e.getMessage());
        }
    }
    
    @Async
    @Transactional
    public void saveUserSearchLog(UserSearchReq request, long resultCount) {
        try {
            // 키워드 검색 로그
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                saveUserKeywordLog(request.getKeyword().trim(), resultCount);
            }
            
            // 단일 기술스택 검색 로그 (각각 개별 저장)
            if (request.getTechStacks() != null) {
                for (String techStack : request.getTechStacks()) {
                    saveUserTechStackLog(techStack, resultCount);
                }
            }
            
            // 단일 기술파트 검색 로그
            if (request.getTechParts() != null) {
                for (String techPart : request.getTechParts()) {
                    saveUserTechPartLog(techPart, resultCount);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to save user search log: {}", e.getMessage());
        }
    }

    private void saveKeywordLog(String keyword, long resultCount) {
        SearchEvent event = SearchEvent.builder()
            .keyword(keyword)
            .searchType("project")
            .searchTime(LocalDateTime.now())
            .sessionId(getCurrentSessionId())
            .resultCount((int) resultCount)
            .build();
        searchEventRepository.save(event);
        log.debug("Saved project keyword search log: {}", keyword);
    }
    
    private void saveTechStackLog(String techStack, long resultCount) {
        SearchEvent event = SearchEvent.builder()
            .singleTechStack(techStack)
            .searchType("project")
            .searchTime(LocalDateTime.now())
            .sessionId(getCurrentSessionId())
            .resultCount((int) resultCount)
            .build();
        searchEventRepository.save(event);
        log.debug("Saved project tech stack search log: {}", techStack);
    }
    
    private void saveTechPartLog(String techPart, long resultCount) {
        SearchEvent event = SearchEvent.builder()
            .techPart(techPart)
            .searchType("project")
            .searchTime(LocalDateTime.now())
            .sessionId(getCurrentSessionId())
            .resultCount((int) resultCount)
            .build();
        searchEventRepository.save(event);
        log.debug("Saved project tech part search log: {}", techPart);
    }

    private void saveUserKeywordLog(String keyword, long resultCount) {
        SearchEvent event = SearchEvent.builder()
            .keyword(keyword)
            .searchType("user")
            .searchTime(LocalDateTime.now())
            .sessionId(getCurrentSessionId())
            .resultCount((int) resultCount)
            .build();
        searchEventRepository.save(event);
        log.debug("Saved user keyword search log: {}", keyword);
    }
    
    private void saveUserTechStackLog(String techStack, long resultCount) {
        SearchEvent event = SearchEvent.builder()
            .singleTechStack(techStack)
            .searchType("user")
            .searchTime(LocalDateTime.now())
            .sessionId(getCurrentSessionId())
            .resultCount((int) resultCount)
            .build();
        searchEventRepository.save(event);
        log.debug("Saved user tech stack search log: {}", techStack);
    }
    
    private void saveUserTechPartLog(String techPart, long resultCount) {
        SearchEvent event = SearchEvent.builder()
            .techPart(techPart)
            .searchType("user")
            .searchTime(LocalDateTime.now())
            .sessionId(getCurrentSessionId())
            .resultCount((int) resultCount)
            .build();
        searchEventRepository.save(event);
        log.debug("Saved user tech part search log: {}", techPart);
    }
    
    private String getCurrentSessionId() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession(false);
                return session != null ? session.getId() : "anonymous";
            }
        } catch (Exception e) {
            log.debug("Could not get session ID: {}", e.getMessage());
        }
        return "anonymous";
    }
}