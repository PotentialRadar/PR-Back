package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.*;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.service.TokenService;
import com.potential_radar.PR.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody UserLoginRequest loginRequest) {
        LoginResponse tokens = userService.login(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(tokens);
    }

    @PostMapping("/signup")
    public ResponseEntity<Object> signup(@Valid @RequestBody UserSignupRequest request) {
        User newUser = userService.register(request);
        return ResponseEntity.ok().body(Map.of("message", "회원가입 성공",
                "userId", newUser.getUserId()));
    }


    @PostMapping("/logout")
    public ResponseEntity<Object> logout(Principal principal) {
        if (principal == null) {
            // Spring Security의 FilterChain에서 처리되지만, 만약을 위한 방어 코드
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }
        String email = principal.getName();
        User user = userService.findByEmail(email);
        tokenService.deleteRefreshToken(user.getUserId());
        return ResponseEntity.ok("로그아웃 성공");
    }

    // 개인정보 조회
    @GetMapping("/user/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(Principal principal) {
        String email = principal.getName();
        UserProfileResponse profile = userService.getUserProfile(email);
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
}
