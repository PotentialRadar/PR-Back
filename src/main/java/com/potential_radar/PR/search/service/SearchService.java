package com.potential_radar.PR.search.service;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.dto.*;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.search.repository.UserSearchRepository;
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
            Criteria techPartCriteria = new Criteria("techPart").is(keyword);
            
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

    public SearchResult<ProjectSearchRes> searchProjects(ProjectSearchReq request) {
        long startTime = System.currentTimeMillis();

        try {
            // 프로젝트 인덱스 존재 여부 확인
            long projectCount = projectSearchRepository.count();
            if (projectCount == 0) {
                // 프로젝트가 없으면 빈 결과 반환
                return SearchResult.<ProjectSearchRes>builder()
                        .content(List.of())
                        .totalElements(0)
                        .totalPages(0)
                        .page(request.getPage())
                        .size(request.getSize())
                        .hasNext(false)
                        .hasPrevious(false)
                        .searchTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }
        } catch (Exception e) {
            // 인덱스가 존재하지 않거나 다른 오류가 발생한 경우 빈 결과 반환
            return SearchResult.<ProjectSearchRes>builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .page(request.getPage())
                    .size(request.getSize())
                    .hasNext(false)
                    .hasPrevious(false)
                    .searchTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // ✅ Criteria 방식으로 변경
        Criteria criteria = new Criteria();

        // 모집중인 프로젝트만 검색 대상
        criteria = criteria.and("status").is("recruiting");

        // 통합 검색창: 프로젝트명, 기술 스택, 기술 파트에서 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            
            Criteria keywordCriteria = new Criteria("projectName").contains(keyword)
                    .or(new Criteria("techStacks").contains(keyword))
                    .or(new Criteria("requiredTechParts").contains(keyword));
            
            criteria = criteria.and(keywordCriteria);
        }

        // 기술 파트 다중 선택 필터링
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            Criteria techPartCriteria = null;
            for (String techPart : request.getTechParts()) {
                Criteria partCriteria = new Criteria("requiredTechParts").is(techPart);
                techPartCriteria = (techPartCriteria == null) ? partCriteria : techPartCriteria.or(partCriteria);
            }
            if (techPartCriteria != null) {
                criteria = criteria.and(techPartCriteria);
            }
        }

        // 기술 스택 다중 선택 검색
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            Criteria techStackCriteria = null;
            for (String techStack : request.getTechStacks()) {
                // 정확 일치와 부분 일치 모두 고려
                Criteria exactMatch = new Criteria("techStacks.keyword").is(techStack);
                Criteria partialMatch = new Criteria("techStacks").contains(techStack.toLowerCase());
                Criteria stackCriteria = exactMatch.or(partialMatch);
                
                techStackCriteria = (techStackCriteria == null) ? stackCriteria : techStackCriteria.or(stackCriteria);
            }
            if (techStackCriteria != null) {
                criteria = criteria.and(techStackCriteria);
            }
        }

        // 정렬 기준에 따른 동적 정렬
        Sort sort;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "score";
        switch (sortBy) {
            case "deadline":
                sort = Sort.by(
                        Sort.Order.asc("deadline"),  // 마감일 빠른 순
                        Sort.Order.desc("_score")
                );
                break;
            case "createdAt":
                sort = Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("_score")
                );
                break;
            default: // "score"
                sort = Sort.by(
                        Sort.Order.desc("_score"),
                        Sort.Order.desc("createdAt")
                );
                break;
        }
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Query query = new CriteriaQuery(criteria).setPageable(pageable);

        SearchHits<ProjectSearchDocument> searchHits = elasticsearchOperations.search(query, ProjectSearchDocument.class);

        List<ProjectSearchRes> responses = searchHits.getSearchHits().stream()
                .map(this::convertToProjectResponse)
                .collect(Collectors.toList());

        long searchTime = System.currentTimeMillis() - startTime;

        return SearchResult.<ProjectSearchRes>builder()
                .content(responses)
                .totalElements(searchHits.getTotalHits())
                .totalPages((int) Math.ceil((double) searchHits.getTotalHits() / request.getSize()))
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(request.getPage() < (searchHits.getTotalHits() / request.getSize()))
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

    private ProjectSearchRes convertToProjectResponse(SearchHit<ProjectSearchDocument> hit) {
        ProjectSearchDocument doc = hit.getContent();
        return ProjectSearchRes.builder()
                .projectId(doc.getProjectId())
                .projectName(doc.getProjectName())
                .description(doc.getDescription())
                .techStacks(doc.getTechStacks())
                .requiredTechParts(doc.getRequiredTechParts())
                .status(doc.getStatus())
                .ownerId(doc.getOwnerId())
                .ownerNickname(doc.getOwnerNickname())
                .createdAt(doc.getCreatedAt())
                .matchScore((double) hit.getScore())
                .build();
    }

    public UnifiedSearchRes unifiedSearch(UnifiedSearchReq request) {
        long startTime = System.currentTimeMillis();
        
        SearchResult<UserSearchRes> userResults = null;
        SearchResult<ProjectSearchRes> projectResults = null;
        
        if ("all".equals(request.getSearchType()) || "user".equals(request.getSearchType())) {
            UserSearchReq userReq = UserSearchReq.builder()
                    .keyword(request.getKeyword())
                    .techParts(request.getTechPart() != null ? List.of(request.getTechPart()) : null)
                    .techStacks(request.getTechStacks())
                    .page(request.getPage())
                    .size(request.getSize())
                    .build();
            userResults = searchUsers(userReq);
        }
        
        if ("all".equals(request.getSearchType()) || "project".equals(request.getSearchType())) {
            ProjectSearchReq projectReq = ProjectSearchReq.builder()
                    .keyword(request.getKeyword())
                    .techStacks(request.getTechStacks())
                    .techParts(request.getRequiredTechParts())
                    .sortBy("score") // 기본값 설정
                    .page(request.getPage())
                    .size(request.getSize())
                    .build();
            projectResults = searchProjects(projectReq);
        }
        
        long totalSearchTime = System.currentTimeMillis() - startTime;
        
        return UnifiedSearchRes.builder()
                .users(userResults)
                .projects(projectResults)
                .totalSearchTimeMs(totalSearchTime)
                .build();
    }
    
    public TechTagsRes getTechTags() {
        log.info("Getting tech tags for frontend");
        
        // 기술 파트 목록 (고정)
        List<String> techParts = List.of("Backend", "Frontend", "Mobile", "DevOps", "AI/ML", "Full Stack");
        
        // 인기 기술 스택 조회 (Elasticsearch aggregation 사용)
        List<TechTagsRes.PopularTechStack> popularTechStacks = getPopularTechStacks();
        
        return TechTagsRes.builder()
                .techParts(techParts)
                .popularTechStacks(popularTechStacks)
                .build();
    }
    
    private List<TechTagsRes.PopularTechStack> getPopularTechStacks() {
        try {
            // 모든 사용자의 기술 스택을 조회해서 빈도 계산
            Iterable<UserSearchDocument> allUsers = userSearchRepository.findAll();
            Map<String, Long> techStackCounts = new HashMap<>();
            
            for (UserSearchDocument user : allUsers) {
                if (user.getTechStacks() != null) {
                    for (String techStack : user.getTechStacks()) {
                        techStackCounts.merge(techStack, 1L, Long::sum);
                    }
                }
            }
            
            // 상위 10개 기술 스택 반환
            return techStackCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(entry -> TechTagsRes.PopularTechStack.builder()
                            .name(entry.getKey())
                            .count(entry.getValue())
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to get popular tech stacks: {}", e.getMessage());
            // 기본값 반환
            return List.of(
                TechTagsRes.PopularTechStack.builder().name("Java").count(0L).build(),
                TechTagsRes.PopularTechStack.builder().name("JavaScript").count(0L).build(),
                TechTagsRes.PopularTechStack.builder().name("Python").count(0L).build(),
                TechTagsRes.PopularTechStack.builder().name("React").count(0L).build(),
                TechTagsRes.PopularTechStack.builder().name("Spring Boot").count(0L).build()
            );
        }
    }
}