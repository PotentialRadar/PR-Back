package com.potential_radar.PR.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.time.Duration;

/**
 * 스마트 캐시 설정
 * - 안정적 데이터: 1주일 TTL (인기 검색어, 기술 스택)
 * - 동적 데이터: 5분 TTL (검색 결과)
 * - 메타데이터: 30일 TTL (기술 파트 목록)
 */
@Configuration
@EnableCaching
public class SmartCacheConfig {
    
    @Value("${app.cache.stable-data.duration-hours:168}") // 1주일
    private int stableDataCacheHours;
    
    @Value("${app.cache.search-results.duration-minutes:5}") // 5분
    private int searchResultCacheMinutes;
    
    @Value("${app.cache.metadata.duration-hours:720}") // 30일
    private int metadataCacheHours;

    @Bean("smartCacheManager")
    @Primary
    public CacheManager smartCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        
        // 기본 캐시 설정
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(searchResultCacheMinutes))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();
        
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig);
        
        // 안정적 데이터 캐시 설정 (1주일 TTL)
        RedisCacheConfiguration stableDataConfig = defaultCacheConfig
                .entryTtl(Duration.ofHours(stableDataCacheHours));
        
        builder.withCacheConfiguration("popularData", stableDataConfig);
        builder.withCacheConfiguration("popularKeywords", stableDataConfig);
        builder.withCacheConfiguration("popularTechStacks", stableDataConfig);
        builder.withCacheConfiguration("popularTechParts", stableDataConfig);
        
        // 메타데이터 캐시 설정 (30일 TTL)
        RedisCacheConfiguration metadataConfig = defaultCacheConfig
                .entryTtl(Duration.ofHours(metadataCacheHours));
        
        builder.withCacheConfiguration("techParts", metadataConfig);
        builder.withCacheConfiguration("techPartNames", metadataConfig);
        builder.withCacheConfiguration("techStacks", metadataConfig);
        builder.withCacheConfiguration("techStackNames", metadataConfig);
        
        // 동적 데이터 캐시 설정 (5분 TTL)
        RedisCacheConfiguration dynamicDataConfig = defaultCacheConfig
                .entryTtl(Duration.ofMinutes(searchResultCacheMinutes));
        
        builder.withCacheConfiguration("searchResults", dynamicDataConfig);
        builder.withCacheConfiguration("projectSearchResults", dynamicDataConfig);
        builder.withCacheConfiguration("userSearchResults", dynamicDataConfig);
        builder.withCacheConfiguration("autoCompleteResults", dynamicDataConfig);
        
        return builder.build();
    }
    
    /**
     * 안정적 데이터 전용 캐시 매니저 (1주일 TTL)
     */
    @Bean("stableCacheManager")
    public CacheManager stableCacheManager(RedisConnectionFactory redisConnectionFactory) {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        
        RedisCacheConfiguration stableConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(stableDataCacheHours))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();
        
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(stableConfig)
                .build();
    }
    
    /**
     * 동적 데이터 전용 캐시 매니저 (5분 TTL)
     */
    @Bean("dynamicCacheManager")
    public CacheManager dynamicCacheManager(RedisConnectionFactory redisConnectionFactory) {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        
        RedisCacheConfiguration dynamicConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(searchResultCacheMinutes))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();
        
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(dynamicConfig)
                .build();
    }
}