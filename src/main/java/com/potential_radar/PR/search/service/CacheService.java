package com.potential_radar.PR.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import com.potential_radar.PR.search.dto.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final SearchService searchService;

    @Cacheable(value = "userSearchResults", key = "#request.toString() + '_' + #request.page + '_' + #request.size")
    public SearchResult<UserSearchRes> getCachedUserSearchResults(UserSearchReq request) {
        log.info("Cache miss for user search: {}", request);
        return searchService.searchUsers(request);
    }

    @Cacheable(value = "projectSearchResults", key = "#request.toString() + '_' + #request.page + '_' + #request.size")
    public SearchResult<ProjectSearchRes> getCachedProjectSearchResults(ProjectSearchReq request) {
        log.info("Cache miss for project search: {}", request);
        return searchService.searchProjects(request);
    }

    @CacheEvict(value = {"userSearchResults", "projectSearchResults"}, allEntries = true)
    public void clearAllSearchCache() {
        log.info("Clearing all search cache");
    }

    @CacheEvict(value = "userSearchResults", allEntries = true)
    public void clearUserSearchCache() {
        log.info("Clearing user search cache");
    }

    @CacheEvict(value = "projectSearchResults", allEntries = true)
    public void clearProjectSearchCache() {
        log.info("Clearing project search cache");
    }
}