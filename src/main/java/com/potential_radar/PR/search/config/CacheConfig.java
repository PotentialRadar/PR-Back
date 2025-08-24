package com.potential_radar.PR.search.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "userSearchResults",
                "projectSearchResults",
                "autoCompleteResults",
                "searchHistory",
                "techParts",        // 기술 파트 캐시
                "techPartNames",    // 기술 파트명 리스트 캐시
                "techStacks",       // 기술 스택 캐시
                "techStackNames"    // 기술 스택명 리스트 캐시
        ));
        return cacheManager;
    }
}