package com.potential_radar.PR.search.service;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.dto.*;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final UserSearchRepository userSearchRepository;
    private final ProjectSearchRepository projectSearchRepository;
    
    // 테스트용 메서드
    public long countAllUsers() {
        return userSearchRepository.count();
    }
    
    public Iterable<UserSearchDocument> findAllUsers() {
        return userSearchRepository.findAll();
    }

    public SearchResult<UserSearchRes> searchUsers(UserSearchReq request) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria();
        boolean hasConditions = false;
        log.info("Starting user search with request: {}", request);

        // 키워드 검색이 있는 경우
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            log.info("Searching users with keyword: '{}'", keyword);
            
            // 통합 검색: 닉네임, 기술스택, 기술파트
            Criteria nicknameCriteria = new Criteria("nickname").contains(keyword);
            Criteria techStackCriteria = new Criteria("techStacks").contains(keyword);
            Criteria techPartCriteria = new Criteria("techPart").contains(keyword);
            
            criteria = nicknameCriteria
                    .or(techStackCriteria)
                    .or(techPartCriteria);
            
            hasConditions = true;
            log.info("Keyword search applied: '{}'", keyword);
        } else {
            // 키워드가 없고 다른 필터도 없으면 빈 결과 반환
            boolean hasOtherFilters = (request.getTechParts() != null && !request.getTechParts().isEmpty()) ||
                                    (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) ||
                                    (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty());
            
            if (!hasOtherFilters) {
                // 필터가 전혀 없으면 빈 결과 반환
                criteria = new Criteria("nickname").is("__NO_RESULTS__");
                hasConditions = true;
                log.info("No keyword and no filters, returning empty results");
            } else {
                // 다른 필터가 있으면 기본 검색 가능한 사용자 필터 적용
                criteria = new Criteria("isSearchable").is(true)
                        .and("isSearchOpen").is(true);
                hasConditions = true;
                log.info("No keyword but has other filters, using default searchable filter");
            }
        }

        // 기술 파트 다중 선택 필터링
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            Criteria techPartCriteria = null;
            for (String techPart : request.getTechParts()) {
                Criteria partCriteria = new Criteria("techPart").is(techPart);
                techPartCriteria = (techPartCriteria == null) ? partCriteria : techPartCriteria.or(partCriteria);
            }
            if (techPartCriteria != null) {
                criteria = criteria.and(techPartCriteria);
                hasConditions = true;
            }
        }

        // 기술 스택 다중 선택 검색
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            Criteria techStackCriteria = null;
            for (String techStack : request.getTechStacks()) {
                Criteria exactMatch = new Criteria("techStacks.keyword").is(techStack);
                Criteria partialMatch = new Criteria("techStacks").contains(techStack.toLowerCase());
                Criteria stackCriteria = exactMatch.or(partialMatch);
                
                techStackCriteria = (techStackCriteria == null) ? stackCriteria : techStackCriteria.or(stackCriteria);
            }
            if (techStackCriteria != null) {
                criteria = criteria.and(techStackCriteria);
                hasConditions = true;
            }
        }

        // 경력 다중 선택 필터링
        if (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty()) {
            log.info("Applying experience filter with {} ranges: {}", 
                    request.getExperienceRanges().size(), request.getExperienceRanges());
            
            Criteria experienceCriteria = null;
            for (ExperienceRange experience : request.getExperienceRanges()) {
                Criteria expCriteria = new Criteria("experienceRange").is(experience.name());
                experienceCriteria = (experienceCriteria == null) ? expCriteria : experienceCriteria.or(expCriteria);
                log.info("Added experience criteria: {} (enum: {})", experience.name(), experience);
            }
            
            if (experienceCriteria != null) {
                criteria = criteria.and(experienceCriteria);
                hasConditions = true;
                log.info("Experience filter applied successfully. Final criteria includes: {}", 
                        request.getExperienceRanges().stream()
                                .map(ExperienceRange::name)
                                .reduce((a, b) -> a + " OR " + b)
                                .orElse("none"));
            }
        }

        // 점수와 생성일시 기준으로 정렬
        Sort sort = Sort.by(
                Sort.Order.desc("_score"),
                Sort.Order.desc("createdAt")
        );
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        List<UserSearchRes> responses;
        long totalElements;
        
        log.info("hasConditions: {}", hasConditions);
        
        // 항상 검색 쿼리 실행 (기본 필터 + 추가 조건)
        Query query = new CriteriaQuery(criteria).setPageable(pageable);
        log.info("Executing search query with criteria: {}", criteria.toString());
        SearchHits<UserSearchDocument> searchHits = elasticsearchOperations.search(query, UserSearchDocument.class);
        responses = searchHits.getSearchHits().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        totalElements = searchHits.getTotalHits();
        
        log.info("Search completed. Found {} users, total hits: {}", responses.size(), totalElements);
        
        // 디버깅: 검색된 사용자들의 닉네임 로그
        responses.forEach(user -> log.info("Found user: {}", user.getNickname()));

        long searchTime = System.currentTimeMillis() - startTime;

        return SearchResult.<UserSearchRes>builder()
                .content(responses)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / request.getSize()))
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(request.getPage() < (totalElements / request.getSize()))
                .hasPrevious(request.getPage() > 0)
                .searchTimeMs(searchTime)
                .build();
    }


    private UserSearchRes convertToUserResponse(SearchHit<UserSearchDocument> hit) {
        UserSearchDocument doc = hit.getContent();
        return UserSearchRes.builder()
                .userId(doc.getUserId())
                .nickname(doc.getNickname())
                .techPart(doc.getTechPart())
                .techStacks(doc.getTechStacks())
                .introduction(doc.getIntroduction())
                .profileImage(doc.getProfileImage())
                .experienceRange(doc.getExperienceRange())
                .createdAt(doc.getCreatedAt())
                .matchScore((double) hit.getScore())
                .build();
    }
    
    private UserSearchRes convertToUserDocumentResponse(UserSearchDocument doc) {
        return UserSearchRes.builder()
                .userId(doc.getUserId())
                .nickname(doc.getNickname())
                .techPart(doc.getTechPart())
                .techStacks(doc.getTechStacks())
                .introduction(doc.getIntroduction())
                .profileImage(doc.getProfileImage())
                .experienceRange(doc.getExperienceRange())
                .createdAt(doc.getCreatedAt())
                .matchScore(1.0) // 기본 점수
                .build();
    }

    // 프로젝트 검색 메서드
    public SearchResult<ProjectSearchRes> searchProjects(ProjectSearchReq request) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria();
        boolean hasConditions = false;
        log.info("Starting project search with request: {}", request);

        // 키워드 검색이 있는 경우
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            log.info("Searching projects with keyword: '{}'", keyword);
            
            // 통합 검색: 제목, 설명, 기술파트, 기술스택
            Criteria titleCriteria = new Criteria("title").contains(keyword);
            Criteria descriptionCriteria = new Criteria("description").contains(keyword);
            Criteria techPartCriteria = new Criteria("techParts").contains(keyword);
            Criteria techStackCriteria = new Criteria("techStacks").contains(keyword);
            
            criteria = titleCriteria
                    .or(descriptionCriteria)
                    .or(techPartCriteria)
                    .or(techStackCriteria);
            
            hasConditions = true;
            log.info("Project keyword search applied: '{}'", keyword);
        }

        // 기술 파트 필터
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Applying tech parts filter: {}", request.getTechParts());
            
            Criteria techPartFilter = new Criteria("techParts").in(request.getTechParts());
            
            if (hasConditions) {
                criteria = criteria.and(techPartFilter);
            } else {
                criteria = techPartFilter;
                hasConditions = true;
            }
        }

        // 기술 스택 필터
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            log.info("Applying tech stacks filter: {}", request.getTechStacks());
            
            Criteria techStackFilter = new Criteria("techStacks").in(request.getTechStacks());
            
            if (hasConditions) {
                criteria = criteria.and(techStackFilter);
            } else {
                criteria = techStackFilter;
                hasConditions = true;
            }
        }

        // 프로젝트 상태 필터
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            log.info("Applying status filter: {}", request.getStatuses());
            
            Criteria statusFilter = new Criteria("status").in(request.getStatuses());
            
            if (hasConditions) {
                criteria = criteria.and(statusFilter);
            } else {
                criteria = statusFilter;
                hasConditions = true;
            }
        }

        // 조건이 없으면 빈 결과 반환
        if (!hasConditions) {
            criteria = new Criteria("title").is("__NO_RESULTS__");
            log.info("No search conditions provided, returning empty results");
        }

        // 점수와 생성일시 기준으로 정렬
        Sort sort = Sort.by(
                Sort.Order.desc("_score"),
                Sort.Order.desc("createdAt")
        );
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Query searchQuery = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProjectSearchDocument> searchHits = elasticsearchOperations.search(searchQuery, ProjectSearchDocument.class);

        List<ProjectSearchRes> responses = searchHits.getSearchHits().stream()
                .map(this::convertToProjectResponse)
                .collect(Collectors.toList());

        long totalElements = searchHits.getTotalHits();
        long searchTime = System.currentTimeMillis() - startTime;

        log.info("Project search completed: found {} results in {}ms", responses.size(), searchTime);

        return SearchResult.<ProjectSearchRes>builder()
                .content(responses)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / request.getSize()))
                .currentPage(request.getPage())
                .size(request.getSize())
                .searchTimeMs(searchTime)
                .build();
    }

    // 프로젝트 테스트용 메서드
    public long countAllProjects() {
        return projectSearchRepository.count();
    }
    
    public Iterable<ProjectSearchDocument> findAllProjects() {
        return projectSearchRepository.findAll();
    }

    private ProjectSearchRes convertToProjectResponse(SearchHit<ProjectSearchDocument> hit) {
        ProjectSearchDocument doc = hit.getContent();
        return ProjectSearchRes.builder()
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .techParts(doc.getTechParts())
                .techStacks(doc.getTechStacks())
                .status(doc.getStatus())
                .teamLeaderId(doc.getTeamLeaderId())
                .teamLeaderNickname(doc.getTeamLeaderNickname())
                .recruitCount(doc.getRecruitCount())
                .viewCount(doc.getViewCount())
                .recruitDeadline(doc.getRecruitDeadline())
                .startDate(doc.getStartDate())
                .endDate(doc.getEndDate())
                .createdAt(doc.getCreatedAt())
                .build();
    }

}