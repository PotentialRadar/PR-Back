package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.config.oauth.CustomUserDetails;
import com.potential_radar.PR.user.dto.EmailRequest;
import com.potential_radar.PR.user.dto.EmailVerificationRequest;
import com.potential_radar.PR.user.dto.UserInfoResponse;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.service.EmailAuthService;
import com.potential_radar.PR.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailAuthService emailAuthService;

    @PostMapping("/send-code")
    public ResponseEntity<Void> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
        emailAuthService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Boolean>> verifyCode(@Valid @RequestBody EmailVerificationRequest request) {
        boolean result = emailAuthService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("success", result));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam @NotBlank @Size(min = 2, max = 20) String nickname) {        boolean duplicate = userService.existsByNickName(nickname);
        return ResponseEntity.ok(Map.of("duplicate", duplicate));

    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않았습니다");
        }

        User user = userService.findByEmail(principal.getUsername());

        return ResponseEntity.ok(new UserInfoResponse(user));
    }


}
