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

    // 스마트 캐싱 전략을 구현하는 서비스 (기존 SearchCacheService 대체)
    private final SmartCacheService smartCacheService;

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
            // 다양한 검색 방식을 조합하여 최대한 유연한 검색 제공
            
            // 닉네임과 직무 검색 - 공백 처리 개선
            Criteria userInfoCriteria;
            if (keyword.contains(" ")) {
                userInfoCriteria = new Criteria("nickname").matches(keyword)
                        .or(new Criteria("jobTitle").matches(keyword));
            } else {
                userInfoCriteria = new Criteria("nickname").contains(keyword)
                        .or(new Criteria("jobTitle").contains(keyword));
            }
            
            // 기술스택 검색 - 공백 처리 개선
            Criteria techStackCriteria;
            if (keywordLower.contains(" ")) {
                // 공백이 있는 경우: phrase 필드와 기존 필드 모두 검색
                techStackCriteria = new Criteria("techStacks.phrase").is(keywordLower)
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techStacks").expression("*" + keywordLower.replace(" ", "*") + "*"));
            } else {
                // 공백이 없는 경우: contains, matches, 와일드카드 모두 사용
                techStackCriteria = new Criteria("techStacks").contains(keywordLower)
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techStacks").expression("*" + keywordLower + "*"));
            }
            
            // 기술파트 검색 - 공백 처리 개선
            Criteria techPartCriteria;
            if (keywordLower.contains(" ")) {
                // 공백이 있는 경우: matches와 정확한 expression 사용
                techPartCriteria = new Criteria("techPart").matches(keywordLower)
                        .or(new Criteria("techPart").expression(keywordLower.replace(" ", "*")));
            } else {
                // 공백이 없는 경우: contains, matches, 와일드카드 모두 사용
                techPartCriteria = new Criteria("techPart").contains(keywordLower)
                        .or(new Criteria("techPart").matches(keywordLower))
                        .or(new Criteria("techPart").expression("*" + keywordLower + "*"));
            }
            
            // 모든 조건을 OR로 결합
            Criteria keywordCriteria = userInfoCriteria.or(techStackCriteria).or(techPartCriteria);

            finalCriteria = keywordCriteria;
            hasConditions = true;
        }

        // 모든 필터를 OR 조건으로 통합
        List<Criteria> filterCriteriaList = new ArrayList<>();

        // 기술 파트 필터 추가
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Adding tech parts filter: {}", request.getTechParts());
            // 기술파트가 여러 개인 경우 OR 조건으로 결합
            Criteria techPartCriteria = null;
            for (String techPart : request.getTechParts()) {
                Criteria singlePartCriteria = new Criteria("techPart").matches(techPart);
                if (techPartCriteria == null) {
                    techPartCriteria = singlePartCriteria;
                } else {
                    techPartCriteria = techPartCriteria.or(singlePartCriteria);
                }
            }
            if (techPartCriteria != null) {
                filterCriteriaList.add(techPartCriteria);
            }
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

        // 검색 가능 유저만 노출 (포트폴리오 공개 + 검색 공개)
        finalCriteria = (finalCriteria == null
                ? new Criteria("isPortfolioOpen").is(true)
                : finalCriteria.and(new Criteria("isPortfolioOpen").is(true)))
                .and(new Criteria("isSearchOpen").is(true));
        log.debug("Applied visibility filter (isPortfolioOpen=true AND isSearchOpen=true). hasConditions: {}", hasConditions);

        // 💡 2. 페이징 및 정렬
        Sort sort = createUserSort(request.getSortBy());
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
            // 다양한 검색 방식을 조합하여 최대한 유연한 검색 제공
            
            // 닉네임과 직무 검색 - 공백 처리 개선
            Criteria userInfoCriteria;
            if (keyword.contains(" ")) {
                userInfoCriteria = new Criteria("nickname").matches(keyword)
                        .or(new Criteria("jobTitle").matches(keyword));
            } else {
                userInfoCriteria = new Criteria("nickname").contains(keyword)
                        .or(new Criteria("jobTitle").contains(keyword));
            }
            
            // 기술스택 검색 - 공백 처리 개선
            Criteria techStackCriteria;
            if (keywordLower.contains(" ")) {
                // 공백이 있는 경우: phrase 필드와 기존 필드 모두 검색
                techStackCriteria = new Criteria("techStacks.phrase").is(keywordLower)
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techStacks").expression("*" + keywordLower.replace(" ", "*") + "*"));
            } else {
                // 공백이 없는 경우: contains, matches, 와일드카드 모두 사용
                techStackCriteria = new Criteria("techStacks").contains(keywordLower)
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techStacks").expression("*" + keywordLower + "*"));
            }
            
            // 기술파트 검색 - 공백 처리 개선
            Criteria techPartCriteria;
            if (keywordLower.contains(" ")) {
                // 공백이 있는 경우: matches와 정확한 expression 사용
                techPartCriteria = new Criteria("techPart").matches(keywordLower)
                        .or(new Criteria("techPart").expression(keywordLower.replace(" ", "*")));
            } else {
                // 공백이 없는 경우: contains, matches, 와일드카드 모두 사용
                techPartCriteria = new Criteria("techPart").contains(keywordLower)
                        .or(new Criteria("techPart").matches(keywordLower))
                        .or(new Criteria("techPart").expression("*" + keywordLower + "*"));
            }
            
            // 모든 조건을 OR로 결합
            Criteria keywordCriteria = userInfoCriteria.or(techStackCriteria).or(techPartCriteria);

            finalCriteria = keywordCriteria;
            hasConditions = true;
        }

        // 모든 필터를 OR 조건으로 통합
        List<Criteria> filterCriteriaList = new ArrayList<>();

        // 기술 파트 필터 추가
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Adding tech parts filter: {}", request.getTechParts());
            // 기술파트가 여러 개인 경우 OR 조건으로 결합
            Criteria techPartCriteria = null;
            for (String techPart : request.getTechParts()) {
                Criteria singlePartCriteria = new Criteria("techPart").matches(techPart);
                if (techPartCriteria == null) {
                    techPartCriteria = singlePartCriteria;
                } else {
                    techPartCriteria = techPartCriteria.or(singlePartCriteria);
                }
            }
            if (techPartCriteria != null) {
                filterCriteriaList.add(techPartCriteria);
            }
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

        // 검색 가능 유저만 노출 (포트폴리오 공개 + 검색 공개)
        finalCriteria = (finalCriteria == null
                ? new Criteria("isPortfolioOpen").is(true)
                : finalCriteria.and(new Criteria("isPortfolioOpen").is(true)))
                .and(new Criteria("isSearchOpen").is(true));
        log.debug("Applied visibility filter (isPortfolioOpen=true AND isSearchOpen=true). hasConditions: {}", hasConditions);

        // 페이징 및 정렬
        Sort sort = createUserSort(request.getSortBy());
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
                .likeCount(doc.getLikeCount())
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
                .likeCount(doc.getLikeCount())
                .createdAt(doc.getCreatedAt())
                .matchScore(1.0) // 기본 점수
                .build();
    }


    // -- 프로젝트 검색
    // 프로젝트 검색 메서드 - Elasticsearch 검색 스코어 사용
    public SearchResult<ProjectSearchRes> searchProjects(ProjectSearchReq request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting project search with request: {}", request);

        // 1. 스마트 캐싱: 인기 검색어 판단 및 캐시 확인
        boolean isPopularSearch = smartCacheService.shouldCacheProjectSearch(request);
        if (isPopularSearch) {
            log.info("Popular search detected, checking smart cache");

            SearchResult<ProjectSearchRes> cachedResult = smartCacheService.getCachedProjectSearchResult(request);
            if (cachedResult != null) {
                long cacheHitTime = System.currentTimeMillis() - startTime;
                log.info("Smart Cache HIT: returning cached result for popular search (actual response time: {}ms vs original search time: {}ms)",
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
            log.info("Smart Cache MISS: will execute search and cache result");
        }

        // Criteria 구성 (유저 검색과 동일한 방식)
        Criteria finalCriteria = null;
        boolean hasConditions = false;

        // 키워드 검색 - 기술스택/기술파트는 소문자로 변환하여 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = normalizeSearchKeyword(request.getKeyword().trim());
            String keywordLower = keyword.toLowerCase(); // 기술스택/기술파트용 소문자 키워드
            log.info("Applying keyword filter: '{}' (lowercase: '{}')", keyword, keywordLower);

            // 키워드는 제목, 기술스택, 기술파트에서 검색
            // 다양한 검색 방식을 조합하여 최대한 유연한 검색 제공
            
            // 제목 검색 - 공백이 있는 경우 matches, 없는 경우 contains
            Criteria titleCriteria;
            if (keyword.contains(" ")) {
                titleCriteria = new Criteria("title").matches(keyword)
                        .or(new Criteria("title.text").matches(keyword));
            } else {
                titleCriteria = new Criteria("title").contains(keyword)
                        .or(new Criteria("title.text").contains(keyword));
            }
            
            // 기술스택 검색 - Spring Security 같은 공백 포함 키워드 특별 처리
            Criteria techStackCriteria;
            if (keywordLower.contains(" ")) {
                // 공백이 있는 경우: phrase 필드와 기존 필드 모두 검색
                techStackCriteria = new Criteria("techStacks.phrase").is(keywordLower)
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techStacks").expression("*" + keywordLower.replace(" ", "*") + "*"));
                
                // Spring Security처럼 두 단어인 경우, 각 단어 모두 포함 검색
                String[] words = keywordLower.split("\\s+");
                if (words.length == 2) {
                    Criteria bothWordsCriteria = new Criteria("techStacks").expression("*" + words[0] + "*" + words[1] + "*")
                            .or(new Criteria("techStacks").expression("*" + words[1] + "*" + words[0] + "*"));
                    techStackCriteria = techStackCriteria.or(bothWordsCriteria);
                }
            } else {
                // 공백이 없는 경우: contains, matches, 와일드카드 모두 사용
                techStackCriteria = new Criteria("techStacks").contains(keywordLower)
                        .or(new Criteria("techStacks").matches(keywordLower))
                        .or(new Criteria("techStacks").expression("*" + keywordLower + "*"));
            }
            
            // 기술파트 검색 - 공백 처리 개선
            Criteria techPartCriteria;
            if (keywordLower.contains(" ")) {
                // 공백이 있는 경우: matches와 정확한 expression 사용
                techPartCriteria = new Criteria("techParts").matches(keywordLower)
                        .or(new Criteria("techParts").expression(keywordLower.replace(" ", "*")));
            } else {
                // 공백이 없는 경우: contains, matches, 와일드카드 모두 사용
                techPartCriteria = new Criteria("techParts").contains(keywordLower)
                        .or(new Criteria("techParts").matches(keywordLower))
                        .or(new Criteria("techParts").expression("*" + keywordLower + "*"));
            }
            
            // 모든 조건을 OR로 결합
            Criteria keywordCriteria = titleCriteria.or(techStackCriteria).or(techPartCriteria);

            finalCriteria = keywordCriteria;
            hasConditions = true;
        }

        // 모든 필터를 OR 조건으로 통합
        List<Criteria> filterCriteriaList = new ArrayList<>();

        // 기술 파트 필터 추가
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            log.info("Adding tech parts filter: {}", request.getTechParts());
            // 기술파트가 여러 개인 경우 OR 조건으로 결합
            Criteria techPartCriteria = null;
            for (String techPart : request.getTechParts()) {
                Criteria singlePartCriteria = new Criteria("techParts").matches(techPart);
                if (techPartCriteria == null) {
                    techPartCriteria = singlePartCriteria;
                } else {
                    techPartCriteria = techPartCriteria.or(singlePartCriteria);
                }
            }
            if (techPartCriteria != null) {
                filterCriteriaList.add(techPartCriteria);
            }
        }

        // 기술 스택 필터 추가
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            log.info("Adding tech stacks filter: {}", request.getTechStacks());
            // 기술스택이 여러 개인 경우 OR 조건으로 결합
            Criteria techStackCriteria = null;
            for (String techStack : request.getTechStacks()) {
                Criteria singleStackCriteria = new Criteria("techStacks").matches(techStack);
                if (techStackCriteria == null) {
                    techStackCriteria = singleStackCriteria;
                } else {
                    techStackCriteria = techStackCriteria.or(singleStackCriteria);
                }
            }
            if (techStackCriteria != null) {
                filterCriteriaList.add(techStackCriteria);
            }
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

        // 정렬 조건 설정
        Sort sort = createProjectSort(request.getSortBy());
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

        // 3. 인기 검색어는 스마트 캐시에 저장 (5분 TTL)
        if (isPopularSearch) {
            smartCacheService.cacheProjectSearchResult(request, result);
            log.info("Cached popular search result in smart cache with 5-minute TTL");
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
                .likeCount(doc.getLikeCount())
                .recruitDeadline(doc.getRecruitDeadline())
                .startDate(doc.getStartDate())
                .endDate(doc.getEndDate())
                .createdAt(doc.getCreatedAt())
                .matchScore((double) hit.getScore())
                .build();
    }


    public TechTagsRes getTechTags() {
        log.info("Getting tech tags for frontend (project-based)");

        // 기술 파트 목록 (캐시된 데이터 사용)
        List<String> techParts = techPartService.getAllTechPartNames();

        // 인기 기술 스택 조회 (사용 빈도 기반 - PopularSearchService에서 조회)
        List<String> popularTechStackNames = popularSearchService.getPopularTechStacks();
        
        // TechTagsRes.PopularTechStack 객체로 변환
        List<TechTagsRes.PopularTechStack> popularTechStacks = popularTechStackNames.stream()
                .map(name -> TechTagsRes.PopularTechStack.builder()
                        .name(name)
                        .count(0L) // 사용 빈도는 표시하지 않으므로 0으로 설정
                        .build())
                .collect(Collectors.toList());

        return TechTagsRes.builder()
                .techParts(techParts)
                .techStacks(popularTechStackNames) // 기술 스택 이름 리스트
                .popularTechStacks(popularTechStacks)
                .build();
    }

    public TechTagsRes getUserTechTags() {
        log.info("Getting tech tags for frontend (user-based)");

        // 기술 파트 목록 (캐시된 데이터 사용)
        List<String> techParts = techPartService.getAllTechPartNames();

        // 유저 기반 인기 기술 스택 조회 (사용 빈도 기반 - PopularSearchService에서 조회)
        List<String> popularTechStackNames = popularSearchService.getPopularUserTechStacks();
        
        // TechTagsRes.PopularTechStack 객체로 변환
        List<TechTagsRes.PopularTechStack> popularTechStacks = popularTechStackNames.stream()
                .map(name -> TechTagsRes.PopularTechStack.builder()
                        .name(name)
                        .count(0L) // 사용 빈도는 표시하지 않으므로 0으로 설정
                        .build())
                .collect(Collectors.toList());

        return TechTagsRes.builder()
                .techParts(techParts)
                .techStacks(popularTechStackNames) // 기술 스택 이름 리스트
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
            log.error("Failed to get popular tech stacks from projects: {}", e.getMessage());
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

    private List<TechTagsRes.PopularTechStack> getPopularTechStacksFromUsers() {
        try {
            // 포트폴리오 공개 + 검색 공개된 유저만 조회
            Criteria searchableCriteria = new Criteria("isPortfolioOpen").is(true)
                    .and(new Criteria("isSearchOpen").is(true));
            
            Pageable pageable = PageRequest.of(0, 10_000);
            Query query = new CriteriaQuery(searchableCriteria).setPageable(pageable);
            SearchHits<UserSearchDocument> searchHits = elasticsearchOperations.search(query, UserSearchDocument.class);
            if (searchHits.getTotalHits() > 10_000) {
                log.warn("Searchable users ({}) exceed 10,000; tech stack counts may be underrepresented. Consider switching to terms aggregation.", searchHits.getTotalHits());
            }
            
            Map<String, Long> techStackCounts = new HashMap<>();

            for (SearchHit<UserSearchDocument> hit : searchHits.getSearchHits()) {
                UserSearchDocument user = hit.getContent();
                if (user.getTechStacks() != null) {
                    for (String techStack : new java.util.HashSet<>(user.getTechStacks())) {
                        techStackCounts.merge(techStack, 1L, Long::sum);
                    }
                }
            }

            log.info("Found {} searchable users with {} unique tech stacks", 
                    searchHits.getTotalHits(), techStackCounts.size());

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
            log.error("Failed to get popular tech stacks from users: {}", e.getMessage());
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

    /**
     * 특수 기술스택 검색어를 정규화하는 메서드 (한글 외래어 표기법 포함)
     */
    private Sort createProjectSort(String sortBy) {
        if (sortBy == null) sortBy = "latest";
        
        log.info("Creating project sort with sortBy: '{}'", sortBy);
        
        switch (sortBy.toLowerCase()) {
            case "popular":
                log.info("Using POPULAR sort: likeCount desc, viewCount desc, createdAt desc");
                return Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"));
            case "deadline":
                log.info("Using DEADLINE sort: recruitDeadline asc, createdAt desc");
                return Sort.by(Sort.Order.asc("recruitDeadline"), Sort.Order.desc("createdAt"));
            case "latest":
            default:
                log.info("Using LATEST sort: createdAt desc");
                return Sort.by(Sort.Order.desc("createdAt"));
        }
    }
    
    private Sort createUserSort(String sortBy) {
        if (sortBy == null) sortBy = "latest";
        
        log.info("Creating user sort with sortBy: '{}'", sortBy);
        
        switch (sortBy.toLowerCase()) {
            case "popular":
                log.info("Using USER POPULAR sort: likeCount desc, createdAt desc");
                return Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"));
            case "latest":
            default:
                log.info("Using USER LATEST sort: _score desc, createdAt desc");
                return Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createdAt"));
        }
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null) return keyword;
        
        String normalized = keyword.trim();
        
        // 특수 기술스택 매핑 (한글 포함)
        switch (normalized.toLowerCase()) {
            // 영어 → 표준 영어
            case "spring cloud": case "springcloud": case "스프링클라우드": case "스프링 클라우드": return "Spring Cloud";
            case "objective-c": case "objectivec": case "objc": case "오브젝티브씨": return "Objective-C";
            case "c#": case "csharp": case "c sharp": case "씨샵": case "시샵": case "dotnet": case "C Sharp": case "c-sharp": case "c_sharp": return "C#";
            case "node.js": case "nodejs": case "노드제이에스": case "노드js": return "Node.js";
            case "vue.js": case "vuejs": case "뷰제이에스": case "뷰js": return "Vue.js";
            case "react native": case "reactnative": case "리액트네이티브": case "리액트 네이티브": return "React Native";
            case "next.js": case "nextjs": case "넥스트제이에스": case "넥스트js": return "Next.js";
            case "nuxt.js": case "nuxtjs": case "넉스트제이에스": case "넉스트js": return "Nuxt.js";
            case "express.js": case "expressjs": case "익스프레스제이에스": case "익스프레스js": return "Express.js";
            case "nest.js": case "nestjs": case "네스트제이에스": case "네스트js": return "Nest.js";
            case "d3.js": case "d3js": case "디쓰리제이에스": case "디쓰리js": return "D3.js";
            case "three.js": case "threejs": case "쓰리제이에스": case "쓰리js": return "Three.js";
            case "chart.js": case "chartjs": case "차트제이에스": case "차트js": return "Chart.js";
            case "socket.io": case "socketio": case "소켓아이오": return "Socket.io";
            case "web3.js": case "web3js": case "웹쓰리제이에스": case "웹쓰리js": return "Web3.js";
            
            // 한글 → 영어
            case "자바": return "Java";
            case "자바스크립트": return "JavaScript";
            case "파이썬": return "Python";
            case "리액트": return "React";
            case "뷰": return "Vue";
            case "앵귤러": return "Angular";
            case "노드": return "Node";
            case "익스프레스": return "Express";
            case "스프링": return "Spring";
            case "스프링부트": case "스프링 부트": return "Spring Boot";
            case "스프링프레임워크": case "스프링 프레임워크": return "Spring Framework";
            case "스프링시큐리티": case "스프링 시큐리티": case "spring security": case "springsecurity": return "Spring Security";
            case "스프링데이터jpa": case "스프링 데이터 jpa": return "Spring Data JPA";
            case "하이버네이트": return "Hibernate";
            case "마이바티스": return "MyBatis";
            case "제이피에이": return "JPA";
            case "마이에스큐엘": return "MySQL";
            case "포스트그레스큐엘": case "포스트그레sql": return "PostgreSQL";
            case "몽고디비": return "MongoDB";
            case "레디스": return "Redis";
            case "엘라스틱서치": return "Elasticsearch";
            case "도커": return "Docker";
            case "쿠버네티스": return "Kubernetes";
            case "아마존웹서비스": return "AWS";
            case "타입스크립트": return "TypeScript";
            case "스위프트": return "Swift";
            case "코틀린": return "Kotlin";
            case "플러터": return "Flutter";
            case "그래프큐엘": return "GraphQL";
            case "레스트api": case "레스트 api": return "REST API";
            case "깃": return "Git";
            case "깃허브": case "깃헙": return "GitHub";
            case "깃랩": return "GitLab";
            case "젠킨스": return "Jenkins";
            case "엔진엑스": return "Nginx";
            case "아파치": return "Apache";
            
            // 기술파트 한글
            case "프론트": case "프런트": return "프론트엔드";
            case "백": case "백앤드": return "백엔드"; 
            case "풀": case "풀 스택": return "풀스택";
            case "앱": case "앱개발": return "모바일";
            case "운영": case "인프라": return "데브옵스";
            case "인공지능": case "머신러닝": case "딥러닝": return "AI/ML";
            case "디자인": case "유아이": case "유엑스": return "UI/UX디자인";
            case "테스터": case "품질관리": return "QA/테스터";
            case "기획": case "프로덕트매니저": return "PM/기획";
            
            default: return normalized;
        }
    }
}