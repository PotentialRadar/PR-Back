package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.*;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.dto.editInfo.UpdatedUserProfileResponse;
import com.potential_radar.PR.user.dto.editInfo.UserProfileUpdateRequest;


public interface UserService {

    User register(UserSignupRequest request);

    boolean isEmailDuplicated(String email);

    User findById(Long userId);

    public User findByEmail(String email);

    LoginResponse login(UserLoginRequest request);

    boolean existsbynickname(String nickname);

    // 개인정보 관련 메서드
    UpdatedUserProfileResponse getUserProfile(String email);
    
    void updateUserProfile(String email, UserProfileUpdateRequest request);
    
    void deleteUser(String email);

}
