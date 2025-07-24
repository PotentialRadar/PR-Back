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

@Controller
@RequiredArgsConstructor
public class TokenApiController {
    private final TokenService tokenService;


    @PostMapping("/api/token")
    public ResponseEntity<CreateAccessTokenResponse> createNewAccessToken(@Valid @RequestBody CreateAccessTokenRequest request){
        try{
            String newAccessToken = tokenService.createNewAccessToken(request.getRefreshToken());

            return ResponseEntity.status(HttpStatus.CREATED).body(new CreateAccessTokenResponse(newAccessToken));
        }catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
