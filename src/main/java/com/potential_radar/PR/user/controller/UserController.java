package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.LoginResponse;
import com.potential_radar.PR.user.dto.UserLoginRequest;
import com.potential_radar.PR.user.dto.UserSignupRequest;
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
        try{

            LoginResponse tokens= userService.login(loginRequest);
            return ResponseEntity.status(HttpStatus.OK).body(tokens);

        }catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }catch (Exception e){
            log.error("🔥 로그인 처리 중 서버 오류", e); // 전체 스택 트레이스 보기
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("로그인 처리 중 오류가 발생했습니다");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<Object> signup(@Valid @RequestBody UserSignupRequest request) {
        try{
            User newUser = userService.register(request);
            return ResponseEntity.ok().body(Map.of("message", "회원가입 성공",
                    "userId", newUser.getUserId()));
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 처리 중 오류가 발생했습니다");
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<Object> logout(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
            }
            String email = principal.getName();
            User user = userService.findByEmail(email);
            tokenService.deleteRefreshToken(user.getUserId());
            return ResponseEntity.ok("로그아웃 성공");

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("로그아웃 처리 중 오류가 발생했습니다");
        }
    }

    // 모든 인증이 필요한 요청에선 Authorization: Bearer {AccessToken} 헤더 사용


}
