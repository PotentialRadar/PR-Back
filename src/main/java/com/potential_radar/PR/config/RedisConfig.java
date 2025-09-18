package com.potential_radar.PR.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 📦 Redis 연결 및 설정 클래스
 * 
 * 🤔 이 클래스가 하는 일:
 * - Spring Boot와 Redis 서버를 연결하는 다리 역할
 * - Redis에 데이터를 저장/조회할 때 사용할 RedisTemplate 설정
 * - 데이터 직렬화/역직렬화 방식 정의
 * 
 * 🔧 주요 설정:
 * - @Configuration: "이 클래스는 설정 클래스야"라고 Spring에 알림
 * - @EnableScheduling: 정기적인 작업 실행 허용 (예: 캐시 정리)
 * - @EnableAsync: 비동기 작업 허용 (백그라운드 작업)
 */
@Configuration  // 🏗️ Spring 설정 클래스임을 명시
@EnableScheduling  // ⏰ 스케줄링 기능 활성화 (정기 작업 가능)
@EnableAsync  // 🚀 비동기 처리 활성화 (백그라운드 작업 가능)
public class RedisConfig {
    
    /**
     * 🔧 RedisTemplate Bean 생성 - Redis와 소통하는 핵심 도구!
     * 
     * 🤔 RedisTemplate이 뭔가요?
     * - Redis와 데이터를 주고받을 때 사용하는 도구
     * - 마치 SQL의 JdbcTemplate처럼, Redis용 템플릿
     * - Java 객체를 Redis가 이해할 수 있는 형태로 변환해줌
     * 
     * 💾 직렬화(Serialization)란?
     * - Java 객체를 Redis에 저장할 수 있는 형태로 변환
     * - 예: User 객체 → JSON 문자열 → Redis 저장
     * 
     * @param connectionFactory Spring Boot가 자동으로 만들어주는 연결 공장
     * @return 설정이 완료된 RedisTemplate 객체
     */
    @Bean  // 🏭 "이 메소드가 만든 객체를 Spring 컨테이너에 등록해줘"
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 🔧 1단계: RedisTemplate 객체 생성
        // <String, Object> = Key는 문자열, Value는 모든 타입 가능
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // 🔌 2단계: Redis 서버 연결 정보 설정
        // application.yml의 Redis 설정을 사용해서 연결
        template.setConnectionFactory(connectionFactory);
        
        // 📝 3단계: 데이터 변환 방식 설정 (중요!)
        //
        // ┌─────────────────────────────────────────────────────────┐
        // │ 🔄 **직렬화 설정 - 데이터 변환 방식**                   │
        // │                                                         │
        // │ Key 직렬화: StringRedisSerializer                       │
        // │ ├─ Key는 항상 문자열 (예: "refresh_token:123")          │
        // │ └─ 사람이 읽기 쉬운 형태로 저장                          │
        // │                                                         │
        // │ Value 직렬화: GenericJackson2JsonRedisSerializer        │
        // │ ├─ Value는 JSON 형태로 저장                             │
        // │ ├─ Java 객체 ↔ JSON 자동 변환                          │
        // │ └─ 예: RefreshTokenInfo → {"userId":123, "token":"abc"} │
        // └─────────────────────────────────────────────────────────┘
        
        // 🔑 일반 Key-Value의 Key 직렬화 (문자열로)
        template.setKeySerializer(new StringRedisSerializer());
        
        // 📦 일반 Key-Value의 Value 직렬화 (JSON으로)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // 🗂️ Hash 자료구조의 Key 직렬화 (문자열로)
        // 💡 Hash란? Redis의 특별한 자료구조, 마치 Map<String, Object>와 같음
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 🗃️ Hash 자료구조의 Value 직렬화 (JSON으로)
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // ✅ 4단계: 설정 완료 및 초기화
        template.afterPropertiesSet();
        
        return template;
    }
}