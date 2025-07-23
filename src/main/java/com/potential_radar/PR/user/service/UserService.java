package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.UserSignupRequest;
import com.potential_radar.PR.user.model.User;
import org.springframework.stereotype.Service;


public interface UserService {

    User register(UserSignupRequest request);

    boolean isEmailDuplicated(String email);
}
