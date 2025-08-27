package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.CreateAccessTokenRequest;
import com.potential_radar.PR.user.dto.CreateAccessTokenResponse;
import com.potential_radar.PR.user.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 🎫 토큰 관리 API 컸트롤러
 * 
 * JWT Access Token의 갱신을 담당하는 REST API 컸트롤러입니다.
 * 
 * 주요 기능:
 * - Refresh Token을 사용하여 새로운 Access Token 발급
 * - 만료된 Access Token을 새로고침 없이 재발급
 * 
 * 보안 처리:
 * - Refresh Token 유효성 및 만료 검증
 * - 만료된 Refresh Token 자동 삭제
 * - 401 Unauthorized 상태 코드 반환 (유효하지 않은 토큰)
 * 
 * 사용 예시:
 * POST /api/token
 * {
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
 * }
 */
@Controller
@RequiredArgsConstructor
public class TokenApiController {
    // 🔧 토큰 관리 비즈니스 로직 서비스
    private final TokenService tokenService;


    /**
     * 🔄 새로운 Access Token 발급 API
     * 
     * Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
     * 이 API는 Access Token이 만료되었을 때 프론트엔드에서 호출합니다.
     * 
     * 처리 과정:
     * 1. Refresh Token 유효성 검증
     * 2. Refresh Token이 만료되었으면 DB에서 삭제
     * 3. 유효한 Refresh Token이면 새 Access Token 생성
     * 4. 201 Created 상태코드와 함께 새 토큰 반환
     * 
     * @param request Refresh Token을 포함하는 요청 객체
     * @return 새로운 Access Token을 포함하는 응답 객체
     * @throws InvalidTokenException 만료되거나 유효하지 않은 Refresh Token인 경우
     */
    @PostMapping("/api/token")
    public ResponseEntity<CreateAccessTokenResponse> createNewAccessToken(@Valid @RequestBody CreateAccessTokenRequest request){
        // 🔄 Refresh Token을 사용하여 새 Access Token 생성
        // 예외 발생 시 GlobalExceptionHandler에서 401 Unauthorized로 처리됨
        String newAccessToken = tokenService.createNewAccessToken(request.getRefreshToken());
        
        // 🎆 201 Created 상태코드와 함께 새로운 Access Token 반환
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateAccessTokenResponse(newAccessToken));
    }
}
