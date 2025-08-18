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
        // IllegalArgumentException은 유효하지 않거나 만료된 토큰을 의미하므로 401 Unauthorized가 더 적절합니다.
        String newAccessToken = tokenService.createNewAccessToken(request.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateAccessTokenResponse(newAccessToken));
    }
}
