package com.potential_radar.PR.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 🔄 Refresh Token 엔티티 클래스
 * 
 * JWT Access Token을 재발급하기 위해 사용되는 Refresh Token 정보를 데이터베이스에 저장하는 엔티티입니다.
 * 
 * 보안 특징:
 * - 사용자별 단일 Refresh Token 유지 (unique constraint)
 * - 만료 시간 자동 확인 기능 (isExpired)
 * - UUID 기반 토큰 생성으로 추측 불가
 * - 일반적으로 14일 만료 설정 (Access Token보다 길음)
 * 
 * 데이터베이스 설계:
 * - refresh_tokens 테이블에 저장
 * - user_id에 UNIQUE 제약 조건으로 사용자별 단일 토큰 보장
 * - Instant 타입으로 UTC 기반 정확한 시간 저장
 */
@Entity  // JPA 엔티티 선언
@Table(name = "refresh_tokens")  // 데이터베이스 테이블 명 지정
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA용 기본 생성자 (외부 접근 차단)
@Getter  // 모든 필드에 대한 getter 메소드 자동 생성
public class RefreshToken {
    // 🆔 기본 키 (Auto Increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", updatable = false)
    private Long id;

    // 👤 해당 Refresh Token을 소유한 사용자 ID (UNIQUE 제약)
    // 한 사용자당 하나의 Refresh Token만 유지 가능
    @Column(name="user_id", nullable = false, unique = true)
    private Long userId;

    // 🎫 실제 Refresh Token 값 (UUID 문자열)
    @Column(name="refresh_token", nullable = false)
    private String refreshToken;

    // ⏰ 토큰 만료 일시 (UTC 기준 Instant)
    @Column(name="expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * 🏭 Refresh Token 엔티티 생성자
     * 
     * 새로운 Refresh Token을 생성할 때 사용됩니다.
     * 보통 RefreshTokenService에서 호출됩니다.
     * 
     * @param userId 사용자 ID
     * @param refreshToken UUID 기반의 Refresh Token 문자열
     * @param expiryDate 토큰 만료 일시 (UTC 기준)
     */
    public RefreshToken(Long userId, String refreshToken, Instant expiryDate) {
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.expiryDate = expiryDate;
    }

    /**
     * 🔄 기존 Refresh Token 업데이트
     * 
     * 동일 사용자에 대해 새로운 Refresh Token을 발급할 때 사용됩니다.
     * 새 레코드를 생성하는 대신 기존 레코드를 업데이트하여 데이터베이스 공간을 절약합니다.
     * 
     * @param newRefreshToken 새로운 Refresh Token 문자열
     * @param newExpiryDate 새로운 만료 일시
     * @return 업데이트된 RefreshToken 인스턴스 (메소드 체이닝을 위해)
     */
    public RefreshToken update(String newRefreshToken, Instant newExpiryDate) {
        this.refreshToken = newRefreshToken;
        this.expiryDate = newExpiryDate;
        return this;
    }

    /**
     * ⏰ Refresh Token 만료 여부 확인
     * 
     * 현재 시간과 비교하여 토큰이 만료되었는지 확인합니다.
     * TokenService에서 새 Access Token 발급 전에 호출하여 유효성을 검증합니다.
     * 
     * @return 만료된 경우 true, 유효한 경우 false
     */
    public boolean isExpired(){
        // 현재 시간(UTC)과 비교하여 만료 일시가 지났는지 확인
        return this.expiryDate.isBefore(Instant.now());
    }
}
