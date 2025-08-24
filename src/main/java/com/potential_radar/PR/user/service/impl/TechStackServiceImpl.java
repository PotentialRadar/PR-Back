package com.potential_radar.PR.user.service.impl;

import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.tech.domain.TechStack;
import com.potential_radar.PR.tech.repository.TechStackRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserTechStack;
import com.potential_radar.PR.user.dto.techStack.UserTechStackRequest;
import com.potential_radar.PR.user.dto.techStack.UserTechStackResponse;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
import com.potential_radar.PR.user.service.TechStackService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TechStackServiceImpl implements TechStackService {

    private final UserRepository userRepository;
    private final TechStackRepository techStackRepository;
    private final UserTechStackRepository userTechStackRepository;

    @Override
    public UserTechStackResponse addTechStack(String userEmail, UserTechStackRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        TechStack techStack = techStackRepository.findById(request.getStackId())
                .orElseThrow(() -> new NotFoundException("기술 스택을 찾을 수 없습니다"));

        if (userTechStackRepository.existsByUserAndStack_TechStackId(user, request.getStackId())) {
            throw new IllegalArgumentException("이미 등록된 기술 스택입니다");
        }

        UserTechStack userTechStack = UserTechStack.builder()
                .user(user)
                .stack(techStack)
                .skillLevel(request.getSkillLevel())
                .build();

        UserTechStack saved = userTechStackRepository.save(userTechStack);
        return UserTechStackResponse.from(saved);
    }

    @Override
    public List<UserTechStackResponse> getTechStacks(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        List<UserTechStack> userTechStacks = userTechStackRepository.findByUserWithTechStack(user);
        return userTechStacks.stream()
                .map(UserTechStackResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public UserTechStackResponse updateTechStack(String userEmail, Long userTechStackId, UserTechStackRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        UserTechStack userTechStack = userTechStackRepository.findByUserAndUserTechStackId(user, userTechStackId)
                .orElseThrow(() -> new NotFoundException("기술 스택 정보를 찾을 수 없습니다"));

        if (!userTechStack.getStack().getTechStackId().equals(request.getStackId())) {
            TechStack newTechStack = techStackRepository.findById(request.getStackId())
                    .orElseThrow(() -> new NotFoundException("기술 스택을 찾을 수 없습니다"));

            if (userTechStackRepository.existsByUserAndStack_TechStackId(user, request.getStackId())) {
                throw new IllegalArgumentException("이미 등록된 기술 스택입니다");
            }
            
            userTechStack.setStack(newTechStack);
        }

        userTechStack.setSkillLevel(request.getSkillLevel());
        UserTechStack saved = userTechStackRepository.save(userTechStack);
        return UserTechStackResponse.from(saved);
    }

    @Override
    public void deleteTechStack(String userEmail, Long userTechStackId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        UserTechStack userTechStack = userTechStackRepository.findByUserAndUserTechStackId(user, userTechStackId)
                .orElseThrow(() -> new NotFoundException("기술 스택 정보를 찾을 수 없습니다"));

        userTechStackRepository.delete(userTechStack);
    }
}