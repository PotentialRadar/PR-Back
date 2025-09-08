package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.domain.Provider;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 📧 Redis 기반 이메일 인증 서비스
 * 
 * 기존 데이터베이스 대신 Redis를 사용하여 이메일 인증 코드를 관리합니다.
 * Redis의 TTL(Time To Live) 기능으로 만료된 인증 코드가 자동으로 삭제됩니다.
 * 
 * Redis 저장 구조:
 * - Key: "email_verify:{email}" (예: "email_verify:user@example.com")
 * - Value: EmailVerificationInfo JSON (코드, 생성시간, 시도횟수 등)
 * - TTL: 설정 가능한 만료 시간 (기본 3분)
 * 
 * 보안 기능:
 * - SecureRandom을 사용한 6자리 인증 코드 생성
 * - 시도 횟수 제한 (5회 초과 시 일시 차단)
 * - 자동 만료로 코드 재사용 방지
 * - 이메일 중복 가입 차단
 * - 소셜 로그인 계정과의 충돌 검사
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEmailAuthService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    // 🔑 Redis 키 접두사 - 이메일 인증 코드를 구분하는 네임스페이스
    private static final String EMAIL_VERIFY_PREFIX = "email_verify:";
    
    // 🔑 시도 횟수 추적 키 접두사 - 무차별 공격 방지용
    private static final String ATTEMPT_COUNT_PREFIX = "email_attempt:";
    
    // 🚫 최대 시도 횟수 - 5회 초과 시 일시 차단
    private static final int MAX_ATTEMPTS = 5;
    
    // ⏰ 차단 시간 (분) - 시도 횟수 초과 시 차단 기간
    private static final long BLOCK_DURATION_MINUTES = 10L;

    // 📧 이메일 인증 코드 만료 시간 (설정 파일에서 주입)
    @Value("${email.verification.expiration-minutes:3}")
    private long expirationMinutes;

    /**
     * 📧 이메일 인증 코드 발송 및 유효성 검사
     * 
     * 이메일 주소의 유효성을 검사하고 인증 코드를 발송합니다.
     * 기존 회원 여부와 소셜 로그인 계정 충돌을 사전에 검사합니다.
     * 
     * @param email 인증 코드를 발송할 이메일 주소
     * @throws IllegalArgumentException 이미 가입된 이메일이거나 소셜 로그인 계정인 경우
     */
    public void validateAndSendCode(String email) {
        // 🔍 기존 회원 가입 여부 확인
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getProvider() != Provider.EMAIL) {
                throw new IllegalArgumentException(
                    "이미 " + user.getProvider().name() + " 계정으로 가입된 이메일입니다. 소셜 로그인을 이용해주세요."
                );
            } else {
                throw new IllegalArgumentException("이미 가입된 이메일입니다.");
            }
        });

        // 🚫 시도 횟수 확인 - 무차별 공격 방지
        if (isBlocked(email)) {
            throw new IllegalArgumentException(
                "인증 시도가 너무 많습니다. " + BLOCK_DURATION_MINUTES + "분 후 다시 시도해주세요."
            );
        }

        // 📧 비동기로 인증 코드 발송
        sendCodeAsync(email);
    }

    /**
     * 📧 비동기 인증 코드 발송 메소드
     * 
     * SecureRandom을 사용하여 6자리 인증 코드를 생성하고,
     * Redis에 저장 후 이메일로 발송합니다.
     * 
     * @param email 인증 코드를 발송할 이메일 주소
     */
    @Async
    public void sendCodeAsync(String email) {
        // 🔢 6자리 보안 인증 코드 생성 (000000 ~ 999999)
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        
        // 🗑️ 기존 인증 코드가 있다면 삭제 (새로운 코드로 교체)
        deleteExistingCode(email);
        
        // 📦 인증 코드 정보 객체 생성
        EmailVerificationInfo verificationInfo = EmailVerificationInfo.builder()
            .email(email)
            .code(code)
            .createdAt(System.currentTimeMillis())
            .attemptCount(0)
            .isVerified(false)
            .build();
        
        // 💾 Redis에 인증 코드 저장 (TTL로 자동 만료)
        String key = EMAIL_VERIFY_PREFIX + email;
        redisTemplate.opsForValue().set(key, verificationInfo, expirationMinutes, TimeUnit.MINUTES);
        
        // 📧 이메일 발송
        sendVerificationEmail(email, code);
        
        log.info("📧 이메일 인증 코드 발송 완료: {} (만료: {}분)", email, expirationMinutes);
    }

    /**
     * ✅ 이메일 인증 코드 검증 메소드
     * 
     * 사용자가 입력한 인증 코드를 검증합니다.
     * 시도 횟수를 추적하여 무차별 공격을 방지합니다.
     * 
     * @param email 검증할 이메일 주소
     * @param inputCode 사용자가 입력한 인증 코드
     * @return 인증 성공 시 true, 실패 시 false
     */
    public boolean verifyCode(String email, String inputCode) {
        String key = EMAIL_VERIFY_PREFIX + email;
        EmailVerificationInfo verificationInfo = 
            (EmailVerificationInfo) redisTemplate.opsForValue().get(key);
        
        // 🚫 인증 코드가 존재하지 않음 (만료되었거나 발송되지 않음)
        if (verificationInfo == null) {
            log.warn("❌ 인증 코드 조회 실패: {} (만료되었거나 발송되지 않음)", email);
            return false;
        }
        
        // 🚫 이미 인증 완료된 코드인지 확인
        if (verificationInfo.getIsVerified()) {
            log.warn("⚠️ 이미 사용된 인증 코드 재사용 시도: {}", email);
            return false;
        }
        
        // 🔢 시도 횟수 증가
        verificationInfo.setAttemptCount(verificationInfo.getAttemptCount() + 1);
        
        // ✅ 인증 코드 일치 여부 확인
        boolean isCorrect = verificationInfo.getCode().equals(inputCode);
        
        if (isCorrect) {
            // 🎉 인증 성공 - 사용됨으로 표시하고 삭제
            verificationInfo.setIsVerified(true);
            redisTemplate.delete(key);
            resetAttemptCount(email);
            log.info("✅ 이메일 인증 성공: {}", email);
            return true;
        } else {
            // ❌ 인증 실패 - 시도 횟수 업데이트
            if (verificationInfo.getAttemptCount() >= MAX_ATTEMPTS) {
                // 🚫 최대 시도 횟수 초과 - 인증 코드 삭제 및 차단
                redisTemplate.delete(key);
                blockEmail(email);
                log.warn("🚫 인증 시도 횟수 초과로 차단: {} ({}회 시도)", email, MAX_ATTEMPTS);
            } else {
                // 📊 시도 횟수만 업데이트
                redisTemplate.opsForValue().set(key, verificationInfo, expirationMinutes, TimeUnit.MINUTES);
                log.warn("❌ 인증 코드 불일치: {} (시도: {}/{})", 
                    email, verificationInfo.getAttemptCount(), MAX_ATTEMPTS);
            }
            return false;
        }
    }

    /**
     * 🔍 특정 이메일의 인증 상태 확인
     * 
     * 인증 코드가 아직 유효한지 확인합니다. (관리용)
     * 
     * @param email 확인할 이메일 주소
     * @return 유효한 인증 코드가 있으면 true
     */
    public boolean hasPendingVerification(String email) {
        String key = EMAIL_VERIFY_PREFIX + email;
        EmailVerificationInfo verificationInfo = 
            (EmailVerificationInfo) redisTemplate.opsForValue().get(key);
        
        return verificationInfo != null && !verificationInfo.getIsVerified();
    }

    /**
     * 🗑️ 특정 이메일의 인증 코드 강제 삭제 (관리용)
     * 
     * @param email 삭제할 이메일 주소
     */
    public void clearVerificationCode(String email) {
        String key = EMAIL_VERIFY_PREFIX + email;
        redisTemplate.delete(key);
        resetAttemptCount(email);
        log.info("🗑️ 이메일 인증 코드 강제 삭제: {}", email);
    }

    // === 🔧 내부 헬퍼 메소드들 ===

    /**
     * 🗑️ 기존 인증 코드 삭제 (내부용)
     */
    private void deleteExistingCode(String email) {
        String key = EMAIL_VERIFY_PREFIX + email;
        redisTemplate.delete(key);
    }

    /**
     * 📧 실제 인증 이메일 발송 (내부용)
     */
    private void sendVerificationEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[PR] 이메일 인증번호");
            message.setText(
                "인증번호: " + code + "\n" +
                "(" + expirationMinutes + "분 이내 입력해주세요)\n\n" +
                "※ 본인이 요청하지 않았다면 이 이메일을 무시해주세요."
            );
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("🔥 이메일 발송 실패: {}", email, e);
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 🚫 이메일 차단 여부 확인 (내부용)
     */
    private boolean isBlocked(String email) {
        String attemptKey = ATTEMPT_COUNT_PREFIX + email;
        Object attempts = redisTemplate.opsForValue().get(attemptKey);
        return attempts != null;
    }

    /**
     * 🚫 이메일 일시 차단 설정 (내부용)
     */
    private void blockEmail(String email) {
        String attemptKey = ATTEMPT_COUNT_PREFIX + email;
        redisTemplate.opsForValue().set(attemptKey, true, BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 🔄 시도 횟수 초기화 (내부용)
     */
    private void resetAttemptCount(String email) {
        String attemptKey = ATTEMPT_COUNT_PREFIX + email;
        redisTemplate.delete(attemptKey);
    }

    /**
     * 📦 이메일 인증 정보를 담는 내부 클래스
     * 
     * Redis에 저장될 이메일 인증 정보를 구조화합니다.
     * 인증 상태 추적과 보안 기능에 필요한 메타데이터를 포함합니다.
     */
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EmailVerificationInfo {
        private String email;          // 📧 대상 이메일 주소
        private String code;           // 🔢 6자리 인증 코드
        private Long createdAt;        // 📅 생성 시간 (타임스탬프)
        private Integer attemptCount;  // 🔢 검증 시도 횟수
        private Boolean isVerified;    // ✅ 인증 완료 여부
    }
}