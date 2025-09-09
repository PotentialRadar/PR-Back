package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.config.oauth.CustomUserDetails;
import com.potential_radar.PR.user.dto.EmailRequest;
import com.potential_radar.PR.user.dto.EmailVerificationRequest;
import com.potential_radar.PR.user.dto.UserInfoResponse;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.service.RedisEmailAuthService;
import com.potential_radar.PR.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * 🔐 인증 및 사용자 관리 API 컸트롤러
 * 
 * 사용자 인증, 이메일 인증, 닉네임 중복 검사 등
 * 사용자 관련 기본 인증 기능을 제공하는 REST API 컸트롤러입니다.
 * 
 * 주요 기능:
 * - 이메일 인증 코드 발송 및 검증
 * - 닉네임 중복 확인
 * - 현재 로그인한 사용자 정보 조회
 * 
 * 보안 전략:
 * - JWT 토큰 기반 인증 사용
 * - Principal 객체를 통한 사용자 식별
 * - 인증되지 않은 사용자에게 401 Unauthorized 반환
 * 
 * API 기본 경로: /api/user/*
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    // 🔧 의존성 주입받는 서비스들
    private final UserService userService;         // 사용자 관리 비즈니스 로직
    private final RedisEmailAuthService emailAuthService;  // Redis 기반 이메일 인증 서비스

    /**
     * 📧 이메일 인증 코드 발송 API
     * 
     * 회원가입 시 이메일 주소 인증을 위해 6자리 인증 코드를 발솤합니다.
     * 이메일 중복 검사와 유효성 검증도 함께 수행됩니다.
     * 
     * @param request 인증 코드를 발송할 이메일 주소
     * @return 200 OK (성공 시) 또는 400 Bad Request (중복/유효하지 않은 이메일)
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
        // 📧 이메일 유효성 검사 및 인증 코드 발송
        emailAuthService.validateAndSendCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ 이메일 인증 코드 검증 API
     * 
     * 사용자가 입력한 6자리 인증 코드가 이메일로 발송된 코드와 일치하는지 검증합니다.
     * 인증 코드는 일정 시간 후 만료되며, 만료된 코드는 검증에 실패합니다.
     * 
     * @param request 이메일 주소와 인증 코드를 포함하는 요청 객체
     * @return JSON 형태의 검증 결과 (success: true/false)
     */
    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Boolean>> verifyCode(@Valid @RequestBody EmailVerificationRequest request) {
        // ✅ 이메일 인증 코드 검증 수행
        boolean result = emailAuthService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("success", result));
    }

    /**
     * 🏷️ 닉네임 중복 확인 API
     * 
     * 회원가입 시 사용자가 입력한 닉네임이 이미 사용 중인지 확인합니다.
     * 닉네임은 2~20자 사이여야 하며, 빈 문자열이 아니어야 합니다.
     * 
     * @param nickname 중복 여부를 확인할 닉네임 (2~20자, 공백 불가)
     * @return JSON 형태의 중복 여부 결과 (duplicate: true/false)
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(
            @RequestParam @NotBlank @Size(min = 2, max = 20) String nickname) {
        // 🔍 닉네임 중복 여부 확인
        boolean duplicate = userService.existsbynickname(nickname);
        return ResponseEntity.ok(Map.of("duplicate", duplicate));
    }

    /**
     * 👤 현재 로그인한 사용자 정보 조회 API
     * 
     * JWT 토큰에서 사용자 정보를 추출하여 현재 로그인한 사용자의 상세 정보를 반환합니다.
     * 인증되지 않은 사용자는 접근할 수 없으며, 401 오류를 반환합니다.
     * 
     * 사용 예시:
     * - 프론트엔드에서 로그인 상태 확인
     * - 사용자 프로필 페이지에서 기본 정보 표시
     * - 마이페이지에서 사용자 정보 로딩
     * 
     * @param principal Spring Security에서 자동 주입되는 인증 주체 (이메일 정보 포함)
     * @return 사용자 정보 DTO 또는 401 Unauthorized
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        log.info("🔍 /user/me 호출됨 - Principal: {}", principal);
        
        // 🚫 인증 정보 없음 검사
        if (principal == null) {
            log.error("❌ Principal이 null입니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않았습니다");
        }

        // 👤 사용자 정보 조회 및 로깅
        log.info("✅ Principal 인증 성공: {}", principal.getName());
        User user = userService.findByEmail(principal.getName());

        // 📊 사용자 정보 DTO로 변환하여 반환
        return ResponseEntity.ok(new UserInfoResponse(user));
    }



}
