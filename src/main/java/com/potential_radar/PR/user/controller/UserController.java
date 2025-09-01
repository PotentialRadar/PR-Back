package com.potential_radar.PR.user.controller;

// === 도메인 및 DTO 관련 Import ===
import com.potential_radar.PR.user.domain.User; // 사용자 엔티티 클래스 (DB 테이블과 매핑)
import com.potential_radar.PR.user.dto.*; // 사용자 관련 DTO들 (Data Transfer Object - 데이터 전송용 객체)
import com.potential_radar.PR.like.service.LikeService; // 좋아요 기능 비즈니스 로직 서비스
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse; // 프로젝트 모집 정보 응답 DTO
import com.potential_radar.PR.user.dto.LoginResponse; // 로그인 응답 DTO (토큰 정보 포함)
import com.potential_radar.PR.user.dto.UserLoginRequest; // 로그인 요청 DTO (이메일, 비밀번호)
import com.potential_radar.PR.user.dto.UserSignupRequest; // 회원가입 요청 DTO
import com.potential_radar.PR.user.dto.editInfo.UpdatedUserProfileResponse; // 프로필 조회 응답 DTO
import com.potential_radar.PR.user.dto.editInfo.UserProfileUpdateRequest; // 프로필 수정 요청 DTO

// === 비즈니스 로직 서비스 Import ===
import com.potential_radar.PR.user.service.TokenService; // JWT 토큰 관리 서비스 (생성, 갱신, 삭제)
import com.potential_radar.PR.config.jwt.TokenProvider; // JWT 토큰 생성/검증 유틸리티
import com.potential_radar.PR.user.service.UserService; // 사용자 관련 비즈니스 로직 서비스

// === HTTP 및 쿠키 관련 Import ===
import jakarta.servlet.http.Cookie; // HTTP 쿠키 객체 (토큰을 쿠키에 저장하기 위해)
import jakarta.servlet.http.HttpServletRequest; // HTTP 요청 객체 (클라이언트 요청 정보)
import jakarta.servlet.http.HttpServletResponse; // HTTP 응답 객체 (쿠키 설정 등)

// === 검증 및 보안 관련 Import ===
import jakarta.validation.Valid; // 요청 DTO 유효성 검증 어노테이션
import java.security.Principal; // Spring Security 인증된 사용자 정보 객체

// === Lombok Import ===
import lombok.RequiredArgsConstructor; // final 필드에 대한 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // 로깅 기능 제공 (@Slf4j로 log 객체 자동 생성)

// === Spring Framework Import ===
import org.springframework.beans.factory.annotation.Value; // 설정 파일의 값을 주입받기 위한 어노테이션
import org.springframework.http.HttpStatus; // HTTP 상태 코드 열거형
import org.springframework.http.ResponseCookie; // SameSite 등 확장 속성 지원 쿠키 빌더
import org.springframework.http.ResponseEntity; // HTTP 응답 엔티티 (상태 코드 + 바디)
import org.springframework.web.bind.annotation.*; // REST API 어노테이션들 (@RestController, @PostMapping 등)

// === 자바 기본 라이브러리 Import ===
import java.util.List; // 리스트 자료구조
import java.util.Map; // 맵 자료구조 (JSON 응답 생성용)

/**
 * 🏠 사용자 관련 REST API 컨트롤러
 * 
 * 이 클래스는 사용자 인증, 프로필 관리, 좋아요 기능과 관련된 모든 HTTP 엔드포인트를 처리합니다.
 * 
 * 주요 기능:
 * - 로그인/로그아웃 (JWT 토큰 기반, httpOnly 쿠키 사용)
 * - 회원가입
 * - 인증 상태 확인
 * - 사용자 프로필 조회/수정
 * - 회원 탈퇴
 * - 사용자의 좋아요한 프로젝트 조회
 */
@RestController // REST API 컨트롤러임을 선언 (JSON 응답 자동 변환)
@RequiredArgsConstructor // final 필드들에 대한 생성자 자동 생성 (의존성 주입용)
@RequestMapping("/api") // 모든 메소드의 기본 URL 경로를 "/api"로 설정
@Slf4j // 로깅 기능 활성화 (log.info(), log.error() 등 사용 가능)
public class UserController {

    // === 의존성 주입받는 서비스들 ===
    private final UserService userService; // 사용자 비즈니스 로직 처리 서비스
    private final TokenService tokenService; // JWT 토큰 관리 서비스 (DB에서 refresh token 관리)
    private final LikeService likeService; // 좋아요 기능 비즈니스 로직 서비스
    private final TokenProvider tokenProvider; // JWT 토큰 생성/검증 유틸리티 클래스
    
    // === 설정 값 주입 ===
    @Value("${app.cookie.secure:true}") // application.yml에서 쿠키 보안 설정 값 주입 (기본값: true)
    private boolean cookieSecure; // HTTPS 환경에서만 쿠키 전송 여부 (개발환경: false, 운영환경: true)

    /**
     * 🔐 로그인 API
     * 
     * 사용자 인증을 수행하고 성공 시 JWT 토큰을 httpOnly 쿠키로 설정합니다.
     * 
     * @param loginRequest 로그인 요청 정보 (이메일, 비밀번호)
     * @param response HTTP 응답 객체 (쿠키 설정용)
     * @return 로그인 성공 메시지
     */
    @PostMapping("/login") // POST /api/login 엔드포인트 매핑
    public ResponseEntity<Object> login(@RequestBody UserLoginRequest loginRequest, HttpServletResponse response) {
        // 1. 사용자 인증 및 토큰 생성
        LoginResponse tokens = userService.login(loginRequest); // 이메일/비밀번호 검증 후 토큰 생성
        
        // 2. Access Token을 HttpOnly + SameSite 쿠키로 설정
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", tokens.accessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(java.time.Duration.ofMillis(tokenProvider.getJwtProperties().getAccessTokenExpiration()))
                .build();
        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        
        // 3. Refresh Token을 HttpOnly + SameSite 쿠키로 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(java.time.Duration.ofMillis(tokenProvider.getJwtProperties().getRefreshTokenExpiration()))
                .build();
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
        
        // 4. 성공 응답 반환 (토큰은 쿠키로 전달되므로 응답 본문에는 메시지만 포함)
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "로그인 성공"));
    }

    /**
     * ✍️ 회원가입 API
     * 
     * 새로운 사용자를 등록합니다.
     * 
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 닉네임 등)
     * @return 회원가입 성공 메시지 및 사용자 ID
     */
    @PostMapping("/signup") // POST /api/signup 엔드포인트 매핑
    public ResponseEntity<Object> signup(@Valid @RequestBody UserSignupRequest request) {
        // @Valid: 요청 DTO의 유효성 검증 (이메일 형식, 비밀번호 조건 등)
        // @RequestBody: HTTP 요청 본문을 UserSignupRequest 객체로 변환
        
        User newUser = userService.register(request); // 사용자 등록 비즈니스 로직 실행
        return ResponseEntity.ok().body(Map.of("message", "회원가입 성공",
                "userId", newUser.getUserId())); // 성공 응답과 함께 생성된 사용자 ID 반환
    }

    /**
     * 🔍 현재 로그인 상태 확인 API
     * 
     * httpOnly 쿠키 방식에서 프론트엔드가 인증 상태를 확인하기 위한 API입니다.
     * JavaScript에서 직접 쿠키에 접근할 수 없으므로, 서버에서 인증 상태를 알려줍니다.
     * 
     * @param principal Spring Security에서 제공하는 인증된 사용자 정보 (JWT 토큰에서 추출)
     * @return 인증 상태 및 사용자 정보
     */
    @GetMapping("/auth/status") // GET /api/auth/status 엔드포인트 매핑
    public ResponseEntity<Object> getAuthStatus(Principal principal) {
        if (principal == null) { // 인증되지 않은 사용자
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        
        // 인증된 사용자의 정보 조회
        String email = principal.getName(); // JWT 토큰에서 추출한 사용자 이메일
        User user = userService.findByEmail(email); // DB에서 사용자 정보 조회
        
        return ResponseEntity.ok(Map.of(
            "authenticated", true, // 인증 상태: true
            "userId", user.getUserId(), // 사용자 ID
            "email", user.getEmail(), // 사용자 이메일
            "nickname", user.getNickname() // 사용자 닉네임
        ));
    }

    /**
     * 🚪 로그아웃 API
     * 
     * 사용자를 로그아웃 처리하고 모든 토큰을 무효화합니다.
     * httpOnly 쿠키로 저장된 토큰들을 삭제하고, 서버에서 refresh token을 제거합니다.
     * 
     * @param principal 인증된 사용자 정보
     * @param request HTTP 요청 객체 (호스트 정보 추출용)
     * @param response HTTP 응답 객체 (쿠키 삭제용)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout") // POST /api/logout 엔드포인트 매핑
    public ResponseEntity<Object> logout(Principal principal, HttpServletRequest request, HttpServletResponse response) {
        if (principal == null) {
            // Spring Security의 FilterChain에서 이미 처리되지만, 만약을 위한 방어 코드
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }
        
        // 1. 서버에서 Refresh Token 삭제
        String email = principal.getName(); // 인증된 사용자 이메일
        User user = userService.findByEmail(email); // 사용자 정보 조회
        tokenService.deleteRefreshToken(user.getUserId()); // DB에서 Refresh Token 삭제
        
        // 2. 브라우저에서 쿠키 삭제 (다양한 경로/도메인 조합으로 확실히 삭제)
        String host = request.getServerName(); // 현재 호스트명 추출 (localhost)

        //TODO : 유틸로 따로 빼놓기! - 쿠키 삭제 로직을 별도 유틸리티 클래스로 분리 예정
        // TODO: 공부,,,,, 코드 공부,,,,!!!!!!!!!!!!! - 쿠키 삭제 메커니즘에 대한 학습 필요

        // 쿠키 삭제는 동일한 이름, 경로, 도메인으로 빈 값과 MaxAge=0으로 설정해야 합니다.
        // 과거에 다양한 설정으로 쿠키를 생성했을 가능성이 있으므로 모든 조합으로 삭제 시도

        // 2-1) 경로 "/" (도메인 미지정) - 기본 설정
        Cookie atRoot = new Cookie("access_token", ""); // Access Token 쿠키 삭제용 생성
        atRoot.setHttpOnly(true); // JavaScript 접근 차단
        atRoot.setSecure(cookieSecure); // HTTPS 설정
        atRoot.setPath("/"); // 루트 경로
        atRoot.setMaxAge(0); // 즉시 만료 (삭제)
        response.addCookie(atRoot);

        Cookie rtRoot = new Cookie("refresh_token", ""); // Refresh Token 쿠키 삭제용 생성
        rtRoot.setHttpOnly(true);
        rtRoot.setSecure(cookieSecure);
        rtRoot.setPath("/");
        rtRoot.setMaxAge(0); // 즉시 만료
        response.addCookie(rtRoot);

        // 2-2) 경로 "/api" (도메인 미지정) - 과거 API 경로로 설정된 쿠키 삭제용
        Cookie atApi = new Cookie("access_token", "");
        atApi.setHttpOnly(true);
        atApi.setSecure(cookieSecure);
        atApi.setPath("/api"); // API 경로로 설정된 쿠키 삭제
        atApi.setMaxAge(0);
        response.addCookie(atApi);

        Cookie rtApi = new Cookie("refresh_token", "");
        rtApi.setHttpOnly(true);
        rtApi.setSecure(cookieSecure);
        rtApi.setPath("/api");
        rtApi.setMaxAge(0);
        response.addCookie(rtApi);

        // 2-3) 경로 "/" + 현재 호스트 도메인 지정 - 과거 Domain 설정 쿠키 호환
        Cookie atRootDomain = new Cookie("access_token", "");
        atRootDomain.setHttpOnly(true);
        atRootDomain.setSecure(cookieSecure);
        atRootDomain.setPath("/");
        atRootDomain.setMaxAge(0);
        atRootDomain.setDomain(host); // 명시적 도메인 설정으로 생성된 쿠키 삭제
        response.addCookie(atRootDomain);

        Cookie rtRootDomain = new Cookie("refresh_token", "");
        rtRootDomain.setHttpOnly(true);
        rtRootDomain.setSecure(cookieSecure);
        rtRootDomain.setPath("/");
        rtRootDomain.setMaxAge(0);
        rtRootDomain.setDomain(host);
        response.addCookie(rtRootDomain);

        // 2-4) 경로 "/api" + 현재 호스트 도메인 지정 - 과거 Domain+Path 설정 쿠키 호환
        Cookie atApiDomain = new Cookie("access_token", "");
        atApiDomain.setHttpOnly(true);
        atApiDomain.setSecure(cookieSecure);
        atApiDomain.setPath("/api");
        atApiDomain.setMaxAge(0);
        atApiDomain.setDomain(host);
        response.addCookie(atApiDomain);

        Cookie rtApiDomain = new Cookie("refresh_token", "");
        rtApiDomain.setHttpOnly(true);
        rtApiDomain.setSecure(cookieSecure);
        rtApiDomain.setPath("/api");
        rtApiDomain.setMaxAge(0);
        rtApiDomain.setDomain(host);
        response.addCookie(rtApiDomain);

        // 3. 세션이 존재하면 무효화 (JSESSIONID 제거 및 SecurityContext 정리)
        if (request.getSession(false) != null) { // 기존 세션이 있는지 확인 (false: 세션이 없으면 null 반환)
            request.getSession(false).invalidate(); // 세션 무효화 (JSESSIONID 쿠키도 삭제됨)
        }
        
        return ResponseEntity.ok("로그아웃 성공");
    }

    /**
     * 👤 개인정보 조회 API
     * 
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     * 
     * @param principal 인증된 사용자 정보
     * @return 사용자 프로필 정보
     */
    @GetMapping("/user/profile") // GET /api/user/profile 엔드포인트 매핑
    public ResponseEntity<UpdatedUserProfileResponse> getUserProfile(Principal principal) {
        if (principal == null) { // 인증되지 않은 사용자
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized 응답
        }
        String email = principal.getName(); // 인증된 사용자 이메일
        UpdatedUserProfileResponse profile = userService.getUserProfile(email); // 프로필 정보 조회
        return ResponseEntity.ok(profile); // 프로필 정보 반환
    }

    /**
     * ✏️ 프로필 정보 수정 API
     * 
     * 사용자의 프로필 정보(닉네임 등)를 수정합니다.
     * 
     * @param request 프로필 수정 요청 정보
     * @param principal 인증된 사용자 정보
     * @return 수정 성공 메시지
     */
    @PutMapping("/user/profile") // PUT /api/user/profile 엔드포인트 매핑
    public ResponseEntity<Object> updateUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest request, // 유효성 검증된 수정 요청 데이터
            Principal principal) { // 인증된 사용자 정보
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증이 필요합니다."));
        }
        String email = principal.getName();
        userService.updateUserProfile(email, request); // 프로필 업데이트 실행
        return ResponseEntity.ok(Map.of("message", "프로필이 수정되었습니다"));
    }

    /**
     * 🗑️ 회원 탈퇴 API
     * 
     * 현재 로그인한 사용자의 계정을 삭제합니다.
     * 
     * @param principal 인증된 사용자 정보
     * @return 탈퇴 성공 메시지
     */
    @DeleteMapping("/user") // DELETE /api/user 엔드포인트 매핑
    public ResponseEntity<Object> deleteUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증이 필요합니다."));
        }
        String email = principal.getName();
        userService.deleteUser(email); // 사용자 계정 삭제 비즈니스 로직 실행
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다"));
    }

    /**
     * 👍 특정 사용자가 좋아요한 프로젝트 조회 API
     * 
     * 지정된 사용자 ID의 좋아요 프로젝트 목록을 조회합니다.
     * 
     * @param userId 조회할 사용자 ID (URL 경로에서 추출)
     * @return 좋아요한 프로젝트 목록
     */
    @GetMapping("/user/{userId}/likes/projects") // GET /api/user/{userId}/likes/projects 엔드포인트 매핑
    public ResponseEntity<List<ProjectRecruitmentResponse>> getLikedProjects(@PathVariable Long userId) {
        // @PathVariable: URL 경로의 {userId} 부분을 Long userId 매개변수로 추출
        List<ProjectRecruitmentResponse> likedProjects = likeService.getLikedProjects(userId);
        return ResponseEntity.ok(likedProjects);
    }

    /**
     * 👍 내가 좋아요한 프로젝트 조회 API
     * 
     * 현재 로그인한 사용자가 좋아요한 프로젝트 목록을 조회합니다.
     * 
     * @param principal 인증된 사용자 정보
     * @return 내가 좋아요한 프로젝트 목록
     */
    @GetMapping("/users/me/liked-projects") // GET /api/users/me/liked-projects 엔드포인트 매핑
    public ResponseEntity<List<ProjectRecruitmentResponse>> getMyLikedProjects(Principal principal) {
        User user = userService.findByEmail(principal.getName()); // 현재 사용자 정보 조회
        List<ProjectRecruitmentResponse> likedProjects = likeService.getLikedProjects(user.getUserId());
        return ResponseEntity.ok(likedProjects);
    }
}
