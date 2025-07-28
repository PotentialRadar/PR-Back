package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.EmailRequest;
import com.potential_radar.PR.user.dto.EmailVerificationRequest;
import com.potential_radar.PR.user.service.EmailAuthService;
import com.potential_radar.PR.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailAuthService emailAuthService;

    @PostMapping("/send-code")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody EmailRequest request) {
        emailAuthService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Boolean>> verifyCode(@RequestBody EmailVerificationRequest request) {
        boolean result = emailAuthService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("success", result));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        boolean duplicate = userService.existsByNickName(nickname);
        return ResponseEntity.ok(Map.of("duplicate", duplicate));

    }
}
