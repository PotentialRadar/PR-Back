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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
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
        log.info("Starting user search with request: {}", request);

        // 💡 1. 단순한 접근 방식: 모든 조건을 하나의 Criteria로 구성
        Criteria finalCriteria;
        
        // 베이스 조건
        Criteria baseCriteria = new Criteria("isSearchable").is(true)
                .and(new Criteria("isSearchOpen").is(true));

        // 키워드 검색이 있는 경우
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            log.info("Applying keyword filter: '{}'", keyword);
            
            // 키워드 조건 생성 (nickname OR techStacks OR techPart에서 매칭)
            Criteria keywordCriteria = new Criteria("nickname").contains(keyword)
                    .or(new Criteria("techStacks").contains(keyword))
                    .or(new Criteria("techPart").contains(keyword));
            
            // 베이스 조건에 키워드 조건을 결합
            finalCriteria = baseCriteria.and(keywordCriteria);
        } else {
            // 키워드가 없으면 베이스 조건만 사용
            finalCriteria = baseCriteria;
        }

        // 기술 파트 필터
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Applying tech parts filter: {}", request.getTechParts());
            finalCriteria = finalCriteria.and(new Criteria("techPart").in(request.getTechParts()));
        }

        // 기술 스택 필터
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            log.info("Applying tech stacks filter: {}", request.getTechStacks());
            finalCriteria = finalCriteria.and(new Criteria("techStacks.keyword").in(request.getTechStacks()));
        }

        // 경력 필터
        if (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty()) {
            List<String> experienceNames = request.getExperienceRanges().stream()
                    .map(ExperienceRange::name)
                    .collect(Collectors.toList());
            log.info("Applying experience ranges filter: {}", experienceNames);
            finalCriteria = finalCriteria.and(new Criteria("experienceRange").in(experienceNames));
        }

        // 키워드 검색만 있고 다른 필터가 없는 경우 결과가 없으면 빈 결과 반환
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty() &&
            (request.getTechParts() == null || request.getTechParts().isEmpty()) &&
            (request.getTechStacks() == null || request.getTechStacks().isEmpty()) &&
            (request.getExperienceRanges() == null || request.getExperienceRanges().isEmpty())) {
            
            // 키워드만 있는 경우는 반드시 키워드가 매칭되어야 함
            log.info("Keyword-only search for: '{}'", request.getKeyword().trim());
        }

        // 💡 2. 페이징 및 정렬
        Sort sort = Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createdAt"));
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Query query = new CriteriaQuery(finalCriteria).setPageable(pageable);

        log.info("Executing user search query with final criteria");
        SearchHits<UserSearchDocument> searchHits = elasticsearchOperations.search(query, UserSearchDocument.class);

        List<UserSearchRes> responses = searchHits.getSearchHits().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        long totalElements = searchHits.getTotalHits();

        log.info("Search completed. Found {} users, total hits: {}", responses.size(), totalElements);

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

//    public SearchResult<UserSearchRes> searchUsers(UserSearchReq request) {
//        long startTime = System.currentTimeMillis();
//
//        Criteria criteria = new Criteria();
//        boolean hasConditions = false;
//        log.info("Starting user search with request: {}", request);
//
//        // 키워드 검색이 있는 경우
//        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
//            String keyword = request.getKeyword().trim();
//            log.info("Searching users with keyword: '{}'", keyword);
//
//            // 대소문자 구분 없는 검색을 위해 원본과 변형된 키워드로 검색
//            Criteria nicknameCriteria = createCaseInsensitiveCriteria("nickname", keyword);
//            Criteria techStackCriteria = createCaseInsensitiveCriteria("techStacks", keyword);
//            // techPart는 Keyword 필드이므로 부분 문자열 매칭을 위한 특별 처리
//            Criteria techPartCriteria = createTechPartCriteria(keyword);
//
//            criteria = nicknameCriteria
//                    .or(techStackCriteria)
//                    .or(techPartCriteria);
//
//            hasConditions = true;
//            log.info("User keyword search applied with case-insensitive matching");
//        } else {
//            // 키워드가 없고 다른 필터도 없으면 빈 결과 반환
//            boolean hasOtherFilters = (request.getTechParts() != null && !request.getTechParts().isEmpty()) ||
//                                    (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) ||
//                                    (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty());
//
//            if (!hasOtherFilters) {
//                // 필터가 전혀 없으면 빈 결과 반환
//                criteria = new Criteria("nickname").is("__NO_RESULTS__");
//                hasConditions = true;
//                log.info("No keyword and no filters, returning empty results");
//            }
//        }
//
//        // 💡 기술 파트 다중 선택 필터링 (OR 조건)
//        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
//            log.info("Applying tech parts filter: {}", request.getTechParts());
//            criteria = criteria.and(new Criteria("techPart").in(request.getTechParts()));
//            hasConditions = true;
//        }
//
//        // 💡 기술 스택 다중 선택 필터링 (OR 조건) - keyword 필드 사용으로 정확한 매칭
//        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
//            log.info("Applying tech stacks filter: {}", request.getTechStacks());
//            criteria = criteria.and(new Criteria("techStacks.keyword").in(request.getTechStacks()));
//            hasConditions = true;
//        }
//
//        // 💡 경력 다중 선택 필터링 (OR 조건)
//        if (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty()) {
//            log.info("Applying experience filter with {} ranges: {}",
//                    request.getExperienceRanges().size(), request.getExperienceRanges());
//
//            // Enum을 String으로 변환하여 쿼리
//            List<String> experienceNames = request.getExperienceRanges().stream()
//                    .map(ExperienceRange::name)
//                    .collect(Collectors.toList());
//
//            criteria = criteria.and(new Criteria("experienceRange").in(experienceNames));
//            hasConditions = true;
//            log.info("Experience filter applied successfully. Final criteria includes: {}",
//                    String.join(" OR ", experienceNames));
//        }
//
//        // 💡 검색 가능한 유저만 필터링 (기본 조건) - 모든 검색에 적용
//        criteria = criteria.and(new Criteria("isSearchable").is(true))
//                          .and(new Criteria("isSearchOpen").is(true));
//        hasConditions = true;
//
//        // 조건이 없으면 빈 결과 반환
//        if (!hasConditions) {
//            criteria = criteria.and(new Criteria("userId").is(-1)); // 존재하지 않는 ID로 빈 결과 생성
//            log.info("No search conditions provided, returning empty results");
//        }
//
//        // 점수와 생성일시 기준으로 정렬
//        Sort sort = Sort.by(
//                Sort.Order.desc("_score"),
//                Sort.Order.desc("createdAt")
//        );
//
//        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
//
//        log.info("Final search criteria applied. hasConditions: {}", hasConditions);
//
//        // 검색 쿼리 실행
//        Query query = new CriteriaQuery(criteria).setPageable(pageable);
//        log.info("Executing search query with criteria: {}", criteria.toString());
//        SearchHits<UserSearchDocument> searchHits = elasticsearchOperations.search(query, UserSearchDocument.class);
//
//        List<UserSearchRes> responses = searchHits.getSearchHits().stream()
//                .map(this::convertToUserResponse)
//                .collect(Collectors.toList());
//        long totalElements = searchHits.getTotalHits();
//
//        log.info("Search completed. Found {} users, total hits: {}", responses.size(), totalElements);
//
//        // 디버깅: 검색된 사용자들의 닉네임 로그
//        responses.forEach(user -> log.info("Found user: {}", user.getNickname()));
//
//        long searchTime = System.currentTimeMillis() - startTime;
//
//        return SearchResult.<UserSearchRes>builder()
//                .content(responses)
//                .totalElements(totalElements)
//                .totalPages((int) Math.ceil((double) totalElements / request.getSize()))
//                .page(request.getPage())
//                .size(request.getSize())
//                .hasNext(request.getPage() < (totalElements / request.getSize()))
//                .hasPrevious(request.getPage() > 0)
//                .searchTimeMs(searchTime)
//                .build();
//    }


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
        log.info("Starting project search with request: {}", request);
        log.info("DEBUG - TechStacks in request: {}, isEmpty: {}", request.getTechStacks(), 
                 request.getTechStacks() != null ? request.getTechStacks().isEmpty() : "null");

        // 💡 단순한 접근 방식: 조건부 결합으로 Criteria 구성
        Criteria finalCriteria = null;
        
        // 키워드 검색이 있는 경우
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            log.info("Searching projects with keyword: '{}'", keyword);
            
            // 키워드 조건 생성 (title OR description OR techParts OR techStacks OR teamLeaderNickname에서 매칭)
            Criteria keywordCriteria = new Criteria("title").contains(keyword)
                    .or(new Criteria("description").contains(keyword))
                    .or(new Criteria("techParts").contains(keyword))
                    .or(new Criteria("techStacks").contains(keyword))
                    .or(new Criteria("teamLeaderNickname").contains(keyword));
            
            finalCriteria = keywordCriteria;
            log.info("Project keyword search applied: '{}'", keyword);
        }

        // 기술 파트 필터
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Applying tech parts filter: {}", request.getTechParts());
            Criteria techPartFilter = new Criteria("techParts").in(request.getTechParts());
            
            if (finalCriteria != null) {
                finalCriteria = finalCriteria.and(techPartFilter);
            } else {
                finalCriteria = techPartFilter;
            }
        }

        // 기술 스택 필터 - 사용자 검색과 동일한 패턴 사용
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            log.info("Applying tech stacks filter: {}", request.getTechStacks());
            Criteria techStackFilter = new Criteria("techStacks.keyword").in(request.getTechStacks());
            
            if (finalCriteria != null) {
                finalCriteria = finalCriteria.and(techStackFilter);
            } else {
                finalCriteria = techStackFilter;
            }
        }

        // 프로젝트 상태 필터
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            log.info("Applying status filter: {}", request.getStatuses());
            Criteria statusFilter = new Criteria("status").in(request.getStatuses());
            
            if (finalCriteria != null) {
                finalCriteria = finalCriteria.and(statusFilter);
            } else {
                finalCriteria = statusFilter;
            }
        }

        // 조건이 없으면 빈 결과 반환
        if (finalCriteria == null) {
            finalCriteria = new Criteria("title").is("__NO_RESULTS__");
            log.info("No search conditions provided, returning empty results");
        }

        // 점수와 생성일시 기준으로 정렬
        Sort sort = Sort.by(
                Sort.Order.desc("_score"),
                Sort.Order.desc("createdAt")
        );
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Query searchQuery = new CriteriaQuery(finalCriteria).setPageable(pageable);

        log.info("Executing project search query with final criteria");
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
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(request.getPage() < (totalElements / request.getSize()))
                .hasPrevious(request.getPage() > 0)
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