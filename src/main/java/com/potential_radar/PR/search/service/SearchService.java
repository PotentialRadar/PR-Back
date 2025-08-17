package com.potential_radar.PR.search.service;

import com.potential_radar.PR.common.enums.ExperienceRange;
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
import java.util.List;
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

        // 기본 필터: 검색 허용된 사용자만 (검색 가능하고 검색 허용)
        criteria = criteria.and("isSearchable").is(true)
                .and("isSearchOpen").is(true);
        hasConditions = true;

        // 통합 검색창: 닉네임, 기술 스택, 기술 파트에서 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            
            Criteria keywordCriteria = new Criteria("nickname").contains(keyword)
                    .or(new Criteria("techStacks").contains(keyword))
                    .or(new Criteria("techPart").contains(keyword));
            
            criteria = criteria.and(keywordCriteria);
            hasConditions = true;
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
            Criteria experienceCriteria = null;
            for (ExperienceRange experience : request.getExperienceRanges()) {
                Criteria expCriteria = new Criteria("experienceRange").is(experience.name());
                experienceCriteria = (experienceCriteria == null) ? expCriteria : experienceCriteria.or(expCriteria);
            }
            if (experienceCriteria != null) {
                criteria = criteria.and(experienceCriteria);
                hasConditions = true;
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
        
        if (hasConditions) {
            Query query = new CriteriaQuery(criteria).setPageable(pageable);
            SearchHits<UserSearchDocument> searchHits = elasticsearchOperations.search(query, UserSearchDocument.class);
            responses = searchHits.getSearchHits().stream()
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());
            totalElements = searchHits.getTotalHits();
        } else {
            // 조건이 없으면 Repository의 findAll을 사용하여 모든 문서 반환
            log.info("No search conditions, using findAll from repository");
            Iterable<UserSearchDocument> allUsers = userSearchRepository.findAll();
            List<UserSearchDocument> userList = new ArrayList<>();
            allUsers.forEach(userList::add);
            log.info("Found {} users from repository", userList.size());
            
            // 수동으로 페이징 처리
            int start = request.getPage() * request.getSize();
            int end = Math.min(start + request.getSize(), userList.size());
            List<UserSearchDocument> pagedUsers = userList.subList(Math.min(start, userList.size()), end);
            
            responses = pagedUsers.stream()
                    .map(this::convertToUserDocumentResponse)
                    .collect(Collectors.toList());
            totalElements = userList.size();
        }

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
        switch (request.getSortBy()) {
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
}