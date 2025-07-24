package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.LoginResponse;
import com.potential_radar.PR.user.dto.UserLoginRequest;
import com.potential_radar.PR.user.dto.UserSignupRequest;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.service.TokenService;
import com.potential_radar.PR.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest loginRequest) {
        try{
           LoginResponse tokens= userService.login(loginRequest);

           return ResponseEntity.status(HttpStatus.OK).body(tokens);

        }catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

   @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserSignupRequest request) {
       try{
           User newUser = userService.register(request);
           return ResponseEntity.ok().body("회원가입 성공");

       }catch (IllegalArgumentException e){
           return ResponseEntity.badRequest().body(e.getMessage());
       }
   }


   @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal User user) {
        tokenService.deleteRefreshToken(user.getUserId());
        return ResponseEntity.ok("로그아웃 성공");
   }



}
