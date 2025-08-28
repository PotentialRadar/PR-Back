package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.dto.*;
import com.potential_radar.PR.like.service.LikeService;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.user.dto.LoginResponse;
import com.potential_radar.PR.user.dto.UserLoginRequest;
import com.potential_radar.PR.user.dto.UserSignupRequest;
import com.potential_radar.PR.user.dto.editInfo.UpdatedUserProfileResponse;
import com.potential_radar.PR.user.dto.editInfo.UserProfileUpdateRequest;
import com.potential_radar.PR.user.service.TokenService;
import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;
    private final LikeService likeService;
    private final TokenProvider tokenProvider;
    
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody UserLoginRequest loginRequest, HttpServletResponse response) {
        LoginResponse tokens = userService.login(loginRequest);
        
        // Access Token을 HttpOnly 쿠키로 설정
        Cookie accessTokenCookie = new Cookie("access_token", tokens.accessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(cookieSecure); // 환경에 따라 설정
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (tokenProvider.getJwtProperties().getAccessTokenExpiration() / 1000));
        // 도메인 설정 제거 - 브라우저가 자동으로 현재 도메인:포트를 사용하도록
        // accessTokenCookie.setDomain("localhost");
        response.addCookie(accessTokenCookie);
        
        // Refresh Token을 HttpOnly 쿠키로 설정
        Cookie refreshTokenCookie = new Cookie("refresh_token", tokens.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(cookieSecure); // 환경에 따라 설정
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (tokenProvider.getJwtProperties().getRefreshTokenExpiration() / 1000));
        // 도메인 설정 제거 - 브라우저가 자동으로 현재 도메인:포트를 사용하도록
        // refreshTokenCookie.setDomain("localhost");
        response.addCookie(refreshTokenCookie);
        
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "로그인 성공"));
    }

    @PostMapping("/signup")
    public ResponseEntity<Object> signup(@Valid @RequestBody UserSignupRequest request) {
        User newUser = userService.register(request);
        return ResponseEntity.ok().body(Map.of("message", "회원가입 성공",
                "userId", newUser.getUserId()));
    }


    // 현재 로그인 상태 확인 API
    @GetMapping("/auth/status")
    public ResponseEntity<Object> getAuthStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        String email = principal.getName();
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "userId", user.getUserId(),
            "email", user.getEmail(),
            "nickname", user.getNickname()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(Principal principal, HttpServletRequest request, HttpServletResponse response) {
        if (principal == null) {
            // Spring Security의 FilterChain에서 처리되지만, 만약을 위한 방어 코드
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }
        String email = principal.getName();
        User user = userService.findByEmail(email);
        tokenService.deleteRefreshToken(user.getUserId());
        
        // Access/Refresh Token 쿠키 삭제 (경로/도메인 변형 모두 만료)
        String host = request.getServerName();

        // 1) 경로 "/" (도메인 미지정)
        Cookie atRoot = new Cookie("access_token", "");
        atRoot.setHttpOnly(true);
        atRoot.setSecure(cookieSecure);
        atRoot.setPath("/");
        atRoot.setMaxAge(0);
        response.addCookie(atRoot);

        Cookie rtRoot = new Cookie("refresh_token", "");
        rtRoot.setHttpOnly(true);
        rtRoot.setSecure(cookieSecure);
        rtRoot.setPath("/");
        rtRoot.setMaxAge(0);
        response.addCookie(rtRoot);

        // 2) 경로 "/api" (도메인 미지정) - 과거 경로 호환 삭제용
        Cookie atApi = new Cookie("access_token", "");
        atApi.setHttpOnly(true);
        atApi.setSecure(cookieSecure);
        atApi.setPath("/api");
        atApi.setMaxAge(0);
        response.addCookie(atApi);

        Cookie rtApi = new Cookie("refresh_token", "");
        rtApi.setHttpOnly(true);
        rtApi.setSecure(cookieSecure);
        rtApi.setPath("/api");
        rtApi.setMaxAge(0);
        response.addCookie(rtApi);

        // 3) 경로 "/" + 현재 호스트 도메인 지정(과거 Domain 쿠키 호환)
        Cookie atRootDomain = new Cookie("access_token", "");
        atRootDomain.setHttpOnly(true);
        atRootDomain.setSecure(cookieSecure);
        atRootDomain.setPath("/");
        atRootDomain.setMaxAge(0);
        atRootDomain.setDomain(host);
        response.addCookie(atRootDomain);

        Cookie rtRootDomain = new Cookie("refresh_token", "");
        rtRootDomain.setHttpOnly(true);
        rtRootDomain.setSecure(cookieSecure);
        rtRootDomain.setPath("/");
        rtRootDomain.setMaxAge(0);
        rtRootDomain.setDomain(host);
        response.addCookie(rtRootDomain);

        // 4) 경로 "/api" + 현재 호스트 도메인 지정(과거 Domain+Path 쿠키 호환)
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

        // 세션이 존재하면 무효화 (JSESSIONID 제거 및 SecurityContext 정리)
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        
        return ResponseEntity.ok("로그아웃 성공");
    }

    // 개인정보 조회
    @GetMapping("/user/profile")
    public ResponseEntity<UpdatedUserProfileResponse> getUserProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = principal.getName();
        UpdatedUserProfileResponse profile = userService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    // 프로필 정보 수정 (닉네임 포함)
    @PutMapping("/user/profile")
    public ResponseEntity<Object> updateUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            Principal principal) {
        String email = principal.getName();
        userService.updateUserProfile(email, request);
        return ResponseEntity.ok(Map.of("message", "프로필이 수정되었습니다"));
    }

    // 회원 탈퇴
    @DeleteMapping("/user")
    public ResponseEntity<Object> deleteUser(Principal principal) {
        String email = principal.getName();
        userService.deleteUser(email);
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다"));
    }

    @GetMapping("/user/{userId}/likes/projects")
    public ResponseEntity<List<ProjectRecruitmentResponse>> getLikedProjects(@PathVariable Long userId) {
        List<ProjectRecruitmentResponse> likedProjects = likeService.getLikedProjects(userId);
        return ResponseEntity.ok(likedProjects);
    }

    @GetMapping("/users/me/liked-projects")
    public ResponseEntity<List<ProjectRecruitmentResponse>> getMyLikedProjects(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<ProjectRecruitmentResponse> likedProjects = likeService.getLikedProjects(user.getUserId());
        return ResponseEntity.ok(likedProjects);
    }
}
