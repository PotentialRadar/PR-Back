package com.potential_radar.PR.search.event;

import com.potential_radar.PR.search.service.SearchCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener {
    
    private final SearchCacheService searchCacheService;
    
    @EventListener
    @Async
    public void handleProjectDataChange(Object event) {
        log.info("Project data changed, invalidating related caches");
        searchCacheService.evictProjectSearchCache("");
    }
    
    @EventListener
    @Async 
    public void handleUserDataChange(Object event) {
        log.info("User data changed, invalidating related portfolio caches");
        searchCacheService.evictPortfolioSearchCache("");
    }
}