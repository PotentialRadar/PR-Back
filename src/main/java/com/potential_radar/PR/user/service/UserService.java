package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.LoginResponse;
import com.potential_radar.PR.user.dto.UserLoginRequest;
import com.potential_radar.PR.user.dto.UserSignupRequest;
import com.potential_radar.PR.user.domain.User;


public interface UserService {

    User register(UserSignupRequest request);

    boolean isEmailDuplicated(String email);

    User findById(Long userId);

    public User findByEmail(String email);

    LoginResponse login(UserLoginRequest request);

    boolean existsByNickName(String nickname);

}
