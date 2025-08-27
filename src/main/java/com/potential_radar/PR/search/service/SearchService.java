package com.potential_radar.PR.search.service;

import com.potential_radar.PR.tech.service.TechPartService;
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
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.FieldValue; // terms 쿼리에 필요
import java.util.stream.Collectors; // Collectors에 필요

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

    // Spring Data ElasticSearch에서 제공하는 핵심 컴포넌트로, 복잡한 ElasticSearch 쿼리를 실행하는 데 사용됨
    private final ElasticsearchOperations elasticsearchOperations;

    private final UserSearchRepository userSearchRepository;
    private final ProjectSearchRepository projectSearchRepository;

    // 인기 검색어 목록을 Redis에서 조회하는 서비스
    private final PopularSearchService popularSearchService;

    // 사용자의 검색 활동(키워드, 필터 등)을 데이터베이스에 비동기적으로 기록
    private final SearchEventService searchEventService;

    // 인기 검색어에 대한 검색 결과를 Redis에 캐싱하고 조회하는 역할
    private final SearchCacheService searchCacheService;

    // 프론트엔드에 제공할 기술 분야 태그 목록을 조회
    private final TechPartService techPartService;


    // -- 유저 검색
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

        // 경력 필터가 있을 때는 네이티브 쿼리 사용
        if (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty()) {
            return searchUsersWithNativeQuery(request, startTime);
        }

        // 프로젝트 검색과 동일한 방식으로 수정
        Criteria finalCriteria = null;
        boolean hasConditions = false;

        // 키워드 검색 - 기술스택/기술파트는 소문자로 변환하여 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            String keywordLower = keyword.toLowerCase(); // 기술스택/기술파트용 소문자 키워드
            log.info("Applying keyword filter: '{}' (lowercase: '{}')", keyword, keywordLower);

            // 키워드는 모든 필드에서 검색 (nickname, jobTitle, 기술스택, 기술파트)
            // 공백이 포함된 키워드는 match 쿼리로 처리
            Criteria keywordCriteria;
            if (keyword.contains(" ")) {
                // 공백이 포함된 경우 match 쿼리 사용
                keywordCriteria = new Criteria("nickname").matches(keyword)
                        .or(new Criteria("jobTitle").matches(keyword))
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techPart").matches(keywordLower));
            } else {
                // 단일 단어는 contains 사용
                keywordCriteria = new Criteria("nickname").contains(keyword)
                        .or(new Criteria("jobTitle").contains(keyword))
                        .or(new Criteria("techStacks").contains(keywordLower))
                        .or(new Criteria("techPart").contains(keywordLower));
            }

            finalCriteria = keywordCriteria;
            hasConditions = true;
        }

        // 모든 필터를 OR 조건으로 통합
        List<Criteria> filterCriteriaList = new ArrayList<>();

        // 기술 파트 필터 추가
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Adding tech parts filter: {}", request.getTechParts());
            filterCriteriaList.add(new Criteria("techPart").in(request.getTechParts()));
        }

        // 기술 스택 필터 추가
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            log.info("Adding tech stacks filter: {}", request.getTechStacks());
            filterCriteriaList.add(new Criteria("techStacks").in(request.getTechStacks()));
        }

        // 경력 필터 추가
        if (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty()) {
            List<String> experienceNames = request.getExperienceRanges().stream()
                    .map(ExperienceRange::name)
                    .collect(Collectors.toList());
            log.info("Adding experience ranges filter - Enums: {}, Strings: {}", request.getExperienceRanges(), experienceNames);

            // 여러 값인 경우 OR 조건으로 결합
            if (experienceNames.size() == 1) {
                filterCriteriaList.add(new Criteria("experienceRange").is(experienceNames.get(0)));
            } else {
                Criteria experienceCriteria = new Criteria("experienceRange").is(experienceNames.get(0));
                for (int i = 1; i < experienceNames.size(); i++) {
                    experienceCriteria = experienceCriteria.or(new Criteria("experienceRange").is(experienceNames.get(i)));
                }
                filterCriteriaList.add(experienceCriteria);
            }
        }

        // 모든 필터를 AND 조건으로 결합
        if (!filterCriteriaList.isEmpty()) {
            for (Criteria filterCriteria : filterCriteriaList) {
                if (hasConditions) {
                    finalCriteria = finalCriteria.and(filterCriteria);
                } else {
                    finalCriteria = filterCriteria;
                    hasConditions = true;
                }
            }
        }

        // 조건이 없으면 모든 사용자 반환 (프로젝트 검색과 동일)
        if (!hasConditions) {
            finalCriteria = new Criteria("nickname").exists();
            hasConditions = true;
            log.info("No search conditions, using nickname.exists() to return all users");
        }

        // 검색 가능 유저만 노출 (포트폴리오도 공개된 유저만)
        finalCriteria = (finalCriteria == null
                ? new Criteria("isSearchable").is(true)
                : finalCriteria.and(new Criteria("isSearchable").is(true)))
                .and(new Criteria("isSearchOpen").is(true))
                .and(new Criteria("isPortfolioOpen").is(true));
        log.debug("Applied visibility filter (isSearchable=true AND isSearchOpen=true AND isPortfolioOpen=true). hasConditions: {}", hasConditions);

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
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());
        boolean hasNext = (request.getPage() + 1) < totalPages;

        SearchResult<UserSearchRes> result = SearchResult.<UserSearchRes>builder()
                .content(responses)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(hasNext)
                .hasPrevious(request.getPage() > 0)
                .searchTimeMs(searchTime)
                .build();

        // 사용자 검색 로그 저장
        searchEventService.saveUserSearchLog(request, totalElements);
        
        return result;
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

    private SearchResult<UserSearchRes> searchUsersWithNativeQuery(UserSearchReq request, long startTime) {
        log.info("Using Criteria query for experience range filtering (fallback to regular search)");
        
        // 네이티브 쿼리 대신 일반 Criteria 쿼리 사용
        return searchUsersWithCriteriaQuery(request, startTime);
    }
    
    private SearchResult<UserSearchRes> searchUsersWithCriteriaQuery(UserSearchReq request, long startTime) {
        // 기존 searchUsers 로직과 동일하게 처리 (경력 필터 포함)
        Criteria finalCriteria = null;
        boolean hasConditions = false;

        // 키워드 검색 - 기술스택/기술파트는 소문자로 변환하여 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            String keywordLower = keyword.toLowerCase(); // 기술스택/기술파트용 소문자 키워드
            log.info("Applying keyword filter: '{}' (lowercase: '{}')", keyword, keywordLower);

            // 키워드는 모든 필드에서 검색 (nickname, jobTitle, 기술스택, 기술파트)
            // 공백이 포함된 키워드는 match 쿼리로 처리
            Criteria keywordCriteria;
            if (keyword.contains(" ")) {
                // 공백이 포함된 경우 match 쿼리 사용
                keywordCriteria = new Criteria("nickname").matches(keyword)
                        .or(new Criteria("jobTitle").matches(keyword))
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techPart").matches(keywordLower));
            } else {
                // 단일 단어는 contains 사용
                keywordCriteria = new Criteria("nickname").contains(keyword)
                        .or(new Criteria("jobTitle").contains(keyword))
                        .or(new Criteria("techStacks").contains(keywordLower))
                        .or(new Criteria("techPart").contains(keywordLower));
            }

            finalCriteria = keywordCriteria;
            hasConditions = true;
        }

        // 모든 필터를 OR 조건으로 통합
        List<Criteria> filterCriteriaList = new ArrayList<>();

        // 기술 파트 필터 추가
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Adding tech parts filter: {}", request.getTechParts());
            filterCriteriaList.add(new Criteria("techPart").in(request.getTechParts()));
        }

        // 기술 스택 필터 추가
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            log.info("Adding tech stacks filter: {}", request.getTechStacks());
            filterCriteriaList.add(new Criteria("techStacks").in(request.getTechStacks()));
        }

        // 경력 필터 추가
        if (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty()) {
            List<String> experienceNames = request.getExperienceRanges().stream()
                    .map(ExperienceRange::name)
                    .collect(Collectors.toList());
            log.info("Adding experience ranges filter - Enums: {}, Strings: {}", request.getExperienceRanges(), experienceNames);

            // 여러 값인 경우 OR 조건으로 결합
            if (experienceNames.size() == 1) {
                filterCriteriaList.add(new Criteria("experienceRange").is(experienceNames.get(0)));
            } else {
                Criteria experienceCriteria = new Criteria("experienceRange").is(experienceNames.get(0));
                for (int i = 1; i < experienceNames.size(); i++) {
                    experienceCriteria = experienceCriteria.or(new Criteria("experienceRange").is(experienceNames.get(i)));
                }
                filterCriteriaList.add(experienceCriteria);
            }
        }

        // 모든 필터를 AND 조건으로 결합
        if (!filterCriteriaList.isEmpty()) {
            for (Criteria filterCriteria : filterCriteriaList) {
                if (hasConditions) {
                    finalCriteria = finalCriteria.and(filterCriteria);
                } else {
                    finalCriteria = filterCriteria;
                    hasConditions = true;
                }
            }
        }

        // 조건이 없으면 모든 사용자 반환
        if (!hasConditions) {
            finalCriteria = new Criteria("nickname").exists();
            hasConditions = true;
            log.info("No search conditions, using nickname.exists() to return all users");
        }

        // 검색 가능 유저만 노출 (포트폴리오도 공개된 유저만)
        finalCriteria = (finalCriteria == null
                ? new Criteria("isSearchable").is(true)
                : finalCriteria.and(new Criteria("isSearchable").is(true)))
                .and(new Criteria("isSearchOpen").is(true))
                .and(new Criteria("isPortfolioOpen").is(true));
        log.debug("Applied visibility filter (isSearchable=true AND isSearchOpen=true AND isPortfolioOpen=true). hasConditions: {}", hasConditions);

        // 페이징 및 정렬
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
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());
        boolean hasNext = (request.getPage() + 1) < totalPages;

        SearchResult<UserSearchRes> result = SearchResult.<UserSearchRes>builder()
                .content(responses)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(hasNext)
                .hasPrevious(request.getPage() > 0)
                .searchTimeMs(searchTime)
                .build();

        // 사용자 검색 로그 저장
        searchEventService.saveUserSearchLog(request, totalElements);
        
        return result;
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
                .githubUrl(doc.getGithubUrl())
                .jobTitle(doc.getJobTitle())
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
                .githubUrl(doc.getGithubUrl())
                .jobTitle(doc.getJobTitle())
                .experienceRange(doc.getExperienceRange())
                .createdAt(doc.getCreatedAt())
                .matchScore(1.0) // 기본 점수
                .build();
    }


    // -- 프로젝트 검색
    // 프로젝트 검색 메서드 - Elasticsearch 검색 스코어 사용
    public SearchResult<ProjectSearchRes> searchProjects(ProjectSearchReq request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting project search with request: {}", request);

        // 1. 인기 검색어 캐시 확인
        boolean isPopularSearch = isPopularSearch(request);
        if (isPopularSearch) {
            log.info("Popular search detected, checking Redis cache");

            SearchResult<ProjectSearchRes> cachedResult = searchCacheService.getCachedSearchResult(request);
            if (cachedResult != null) {
                long cacheHitTime = System.currentTimeMillis() - startTime;
                log.info("Cache HIT: returning cached result for popular search (actual response time: {}ms vs original search time: {}ms)",
                        cacheHitTime, cachedResult.getSearchTimeMs());
                searchEventService.saveSearchLog(request, cachedResult.getTotalElements());
                return SearchResult.<ProjectSearchRes>builder()
                        .content(cachedResult.getContent())
                        .totalElements(cachedResult.getTotalElements())
                        .totalPages(cachedResult.getTotalPages())
                        .page(cachedResult.getPage())
                        .size(cachedResult.getSize())
                        .hasNext(cachedResult.isHasNext())
                        .hasPrevious(cachedResult.isHasPrevious())
                        .searchTimeMs(cachedResult.getSearchTimeMs()) // 원래 검색 시간 유지
                        .fromCache(true) // 캐시에서 조회됨
                        .actualResponseTimeMs(cacheHitTime) // 실제 응답 시간
                        .build();
            }
            log.info("Cache MISS: will execute search and cache result");
        }

        // Criteria 구성 (유저 검색과 동일한 방식)
        Criteria finalCriteria = null;
        boolean hasConditions = false;

        // 키워드 검색 - 기술스택/기술파트는 소문자로 변환하여 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            String keywordLower = keyword.toLowerCase(); // 기술스택/기술파트용 소문자 키워드
            log.info("Applying keyword filter: '{}' (lowercase: '{}')", keyword, keywordLower);

            // 키워드는 제목과 기술스택에서만 검색
            Criteria keywordCriteria = new Criteria("title").contains(keyword)
                    .or(new Criteria("title.text").contains(keyword))
                    .or(new Criteria("techStacks").contains(keywordLower));

            finalCriteria = keywordCriteria;
            hasConditions = true;
        }

        // 모든 필터를 OR 조건으로 통합
        List<Criteria> filterCriteriaList = new ArrayList<>();

        // 기술 파트 필터 추가
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Adding tech parts filter: {}", request.getTechParts());
            filterCriteriaList.add(new Criteria("techParts").in(request.getTechParts()));
        }

        // 기술 스택 필터 추가
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            log.info("Adding tech stacks filter: {}", request.getTechStacks());
            filterCriteriaList.add(new Criteria("techStacks").in(request.getTechStacks()));
        }

        // 상태 필터 추가
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            log.info("Adding status filter: {}", request.getStatuses());
            filterCriteriaList.add(new Criteria("status").in(request.getStatuses()));
        }

        // 모든 필터를 AND 조건으로 결합
        if (!filterCriteriaList.isEmpty()) {
            for (Criteria filterCriteria : filterCriteriaList) {
                if (hasConditions) {
                    finalCriteria = finalCriteria.and(filterCriteria);
                } else {
                    finalCriteria = filterCriteria;
                    hasConditions = true;
                }
            }
        }

        // 조건이 없으면 모든 프로젝트 반환
        if (!hasConditions) {
            finalCriteria = new Criteria("projectId").exists();
        }

        // 점수와 생성일시 기준으로 정렬
        Sort sort = Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createdAt"));
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Query query = new CriteriaQuery(finalCriteria).setPageable(pageable);

        log.info("Executing project search query with criteria");
        SearchHits<ProjectSearchDocument> searchHits = elasticsearchOperations.search(query, ProjectSearchDocument.class);

        List<ProjectSearchRes> responses = searchHits.getSearchHits().stream()
                .map(this::convertToProjectResponse)
                .collect(Collectors.toList());
        long totalElements = searchHits.getTotalHits();

        log.info("Project search completed: found {} results", responses.size());

        long searchTime = System.currentTimeMillis() - startTime;
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());
        boolean hasNext = (request.getPage() + 1) < totalPages;

        SearchResult<ProjectSearchRes> result = SearchResult.<ProjectSearchRes>builder()
                .content(responses)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(hasNext)
                .hasPrevious(request.getPage() > 0)
                .searchTimeMs(searchTime)
                .fromCache(false) // 캐시에서 조회되지 않음
                .actualResponseTimeMs(searchTime) // 실제 응답 시간 = 검색 시간
                .build();

        // 2. 비동기 로깅
        searchEventService.saveSearchLog(request, totalElements);

        // 3. 인기 검색어는 Redis에 캐싱
        if (isPopularSearch) {
            searchCacheService.cacheSearchResult(request, result);
            log.info("Cached popular search result in Redis");
        }

        return result;
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
                .matchScore((double) hit.getScore())
                .build();
    }


    public TechTagsRes getTechTags() {
        log.info("Getting tech tags for frontend");

        // 기술 파트 목록 (캐시된 데이터 사용)
        List<String> techParts = techPartService.getAllTechPartNames();

        // 인기 기술 스택 조회 (Elasticsearch aggregation 사용)
        List<TechTagsRes.PopularTechStack> popularTechStacks = getPopularTechStacks();

        // 기술 스택 이름만 추출
        List<String> techStacks = popularTechStacks.stream()
                .map(TechTagsRes.PopularTechStack::getName)
                .collect(Collectors.toList());

        return TechTagsRes.builder()
                .techParts(techParts)
                .techStacks(techStacks) // 기술 스택 이름 리스트 추가
                .popularTechStacks(popularTechStacks)
                .build();
    }

    private List<TechTagsRes.PopularTechStack> getPopularTechStacks() {
        try {
            // 모든 프로젝트의 기술 스택을 조회해서 빈도 계산
            Iterable<ProjectSearchDocument> allProjects = projectSearchRepository.findAll();
            Map<String, Long> techStackCounts = new HashMap<>();

            for (ProjectSearchDocument project : allProjects) {
                if (project.getTechStacks() != null) {
                    for (String techStack : project.getTechStacks()) {
                        techStackCounts.merge(techStack, 1L, Long::sum);
                    }
                }
            }

            // 상위 20개 기술 스택 반환 (20개로 증가)
            return techStackCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(20)
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

    // 인기 검색어 체크
    private boolean isPopularSearch(ProjectSearchReq request) {
        // 인기 키워드 체크
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String searchKeyword = request.getKeyword().trim();
            return popularSearchService.getPopularKeywords().stream()
                    .anyMatch(keyword -> keyword.equalsIgnoreCase(searchKeyword));
        }

        // 단일 인기 기술스택 체크
        if (request.getTechStacks() != null && request.getTechStacks().size() == 1) {
            String techStack = request.getTechStacks().get(0);
            return popularSearchService.getPopularTechStacks().contains(techStack);
        }

        // 단일 인기 기술파트 체크
        if (request.getTechParts() != null && request.getTechParts().size() == 1) {
            String techPart = request.getTechParts().get(0);
            return popularSearchService.getPopularTechParts().contains(techPart);
        }

        return false;
    }



}