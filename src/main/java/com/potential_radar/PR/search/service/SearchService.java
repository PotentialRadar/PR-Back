package com.potential_radar.PR.search.service;

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


import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final UserSearchRepository userSearchRepository;
    private final ProjectSearchRepository projectSearchRepository;

    public SearchResult<UserSearchRes> searchUsers(UserSearchReq request) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria();

        // 검색 허용된 사용자만
        criteria = criteria.and("isSearchable").is(true);

        // 닉네임 검색
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            criteria = criteria.and("nickname").contains(request.getNickname());
        }

        // 기술 파트 필터링
        if (request.getTechPart() != null && !request.getTechPart().trim().isEmpty()) {
            criteria = criteria.and("techPart").is(request.getTechPart());
        }

        // 기술 스택 검색 (부분 일치 및 정확 일치 모두 지원)
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

        // 점수와 생성일시 기준으로 정렬
        Sort sort = Sort.by(
                Sort.Order.desc("_score"),
                Sort.Order.desc("createdAt")
        );
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Query query = new CriteriaQuery(criteria).setPageable(pageable);

        SearchHits<UserSearchDocument> searchHits = elasticsearchOperations.search(query, UserSearchDocument.class);

        List<UserSearchRes> responses = searchHits.getSearchHits().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

        long searchTime = System.currentTimeMillis() - startTime;

        return SearchResult.<UserSearchRes>builder()
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

    public SearchResult<ProjectSearchRes> searchProjects(ProjectSearchReq request) {
        long startTime = System.currentTimeMillis();

        // ✅ Criteria 방식으로 변경
        Criteria criteria = new Criteria();

        // 프로젝트명 검색
        if (request.getProjectName() != null && !request.getProjectName().trim().isEmpty()) {
            criteria = criteria.and("projectName").contains(request.getProjectName());
        }

        // 기술 스택 검색 (부분 일치 및 정확 일치 모두 지원)
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

        // 구하는 기술 파트 필터링
        if (request.getRequiredTechParts() != null && !request.getRequiredTechParts().isEmpty()) {
            Criteria techPartCriteria = null;
            for (String techPart : request.getRequiredTechParts()) {
                Criteria partCriteria = new Criteria("requiredTechParts").is(techPart);
                techPartCriteria = (techPartCriteria == null) ? partCriteria : techPartCriteria.or(partCriteria);
            }
            if (techPartCriteria != null) {
                criteria = criteria.and(techPartCriteria);
            }
        }

        // 점수와 생성일시 기준으로 정렬
        Sort sort = Sort.by(
                Sort.Order.desc("_score"),
                Sort.Order.desc("createdAt")
        );
        
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
                .githubUrl(doc.getGithubUrl())
                .region(doc.getRegion())
                .createdAt(doc.getCreatedAt())
                .matchScore((double) hit.getScore())
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
                    .nickname(request.getKeyword())
                    .techPart(request.getTechPart())
                    .techStacks(request.getTechStacks())
                    .page(request.getPage())
                    .size(request.getSize())
                    .build();
            userResults = searchUsers(userReq);
        }
        
        if ("all".equals(request.getSearchType()) || "project".equals(request.getSearchType())) {
            ProjectSearchReq projectReq = ProjectSearchReq.builder()
                    .projectName(request.getKeyword())
                    .techStacks(request.getTechStacks())
                    .requiredTechParts(request.getRequiredTechParts())
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