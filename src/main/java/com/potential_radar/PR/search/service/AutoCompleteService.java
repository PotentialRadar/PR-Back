package com.potential_radar.PR.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.dto.AutoCompleteRes;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCompleteService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Cacheable(value = "autoCompleteResults", key = "#query + '_' + #type")
    public AutoCompleteRes getAutoCompleteSuggestions(String query, String type) {
        if (query == null || query.trim().length() < 2) {
            return AutoCompleteRes.builder()
                    .suggestions(Collections.emptyList())
                    .build();
        }

        Set<String> suggestions = new HashSet<>();

        switch (type.toLowerCase()) {
            case "user":
                suggestions.addAll(getUserSuggestions(query));
                break;
            case "project":
                suggestions.addAll(getProjectSuggestions(query));
                break;
            case "tech":
                suggestions.addAll(getTechStackSuggestions(query));
                break;
            default:
                suggestions.addAll(getUserSuggestions(query));
                suggestions.addAll(getProjectSuggestions(query));
                suggestions.addAll(getTechStackSuggestions(query));
        }

        List<String> sortedSuggestions = suggestions.stream()
                .limit(10)
                .collect(Collectors.toList());

        return AutoCompleteRes.builder()
                .suggestions(sortedSuggestions)
                .build();
    }

    private Set<String> getUserSuggestions(String query) {
        Set<String> suggestions = new HashSet<>();
        
        Criteria criteria = new Criteria()
                .and("isSearchable").is(true)
                .and("isPortfolioOpen").is(true)
                .and("isSearchOpen").is(true);

        // 닉네임 매칭
        Criteria nicknameCriteria = criteria.and("nickname").contains(query);
        Query nicknameQuery = new CriteriaQuery(nicknameCriteria)
                .setPageable(PageRequest.of(0, 5));

        SearchHits<UserSearchDocument> nicknameHits = 
                elasticsearchOperations.search(nicknameQuery, UserSearchDocument.class);
        
        suggestions.addAll(nicknameHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getNickname())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return suggestions;
    }

    private Set<String> getProjectSuggestions(String query) {
        Set<String> suggestions = new HashSet<>();
        
        Criteria criteria = new Criteria()
                .and("status").is("recruiting")
                .and("projectName").contains(query);

        Query projectQuery = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(0, 5));

        SearchHits<ProjectSearchDocument> projectHits = 
                elasticsearchOperations.search(projectQuery, ProjectSearchDocument.class);
        
        suggestions.addAll(projectHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getProjectName())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return suggestions;
    }

    private Set<String> getTechStackSuggestions(String query) {
        Set<String> suggestions = new HashSet<>();
        
        // 사용자 기술스택에서 검색
        Criteria userCriteria = new Criteria()
                .and("isSearchable").is(true)
                .and("techStacks").contains(query);

        Query userTechQuery = new CriteriaQuery(userCriteria)
                .setPageable(PageRequest.of(0, 10));

        SearchHits<UserSearchDocument> userTechHits = 
                elasticsearchOperations.search(userTechQuery, UserSearchDocument.class);
        
        userTechHits.getSearchHits().forEach(hit -> {
            List<String> techStacks = hit.getContent().getTechStacks();
            if (techStacks != null) {
                suggestions.addAll(techStacks.stream()
                        .filter(tech -> tech.toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toSet()));
            }
        });

        // 프로젝트 기술스택에서 검색
        Criteria projectCriteria = new Criteria()
                .and("status").is("recruiting")
                .and("techStacks").contains(query);

        Query projectTechQuery = new CriteriaQuery(projectCriteria)
                .setPageable(PageRequest.of(0, 10));

        SearchHits<ProjectSearchDocument> projectTechHits = 
                elasticsearchOperations.search(projectTechQuery, ProjectSearchDocument.class);
        
        projectTechHits.getSearchHits().forEach(hit -> {
            List<String> techStacks = hit.getContent().getTechStacks();
            if (techStacks != null) {
                suggestions.addAll(techStacks.stream()
                        .filter(tech -> tech.toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toSet()));
            }
        });

        return suggestions;
    }
}