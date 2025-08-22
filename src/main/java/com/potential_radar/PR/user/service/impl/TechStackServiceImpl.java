package com.potential_radar.PR.user.service.impl;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.techStack.domain.TechStackToDelete;
import com.potential_radar.PR.techStack.repository.TechStackRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserTechStack1;
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

        TechStackToDelete techStackToDelete = techStackRepository.findById(request.getStackId())
                .orElseThrow(() -> new NotFoundException("기술 스택을 찾을 수 없습니다"));

        if (userTechStackRepository.existsByUserAndStack_StackId(user, request.getStackId())) {
            throw new IllegalArgumentException("이미 등록된 기술 스택입니다");
        }

        UserTechStack1 userTechStack = UserTechStack1.builder()
                .user(user)
                .stack(techStackToDelete)
                .skillLevel(request.getSkillLevel())
                .build();

        UserTechStack1 saved = userTechStackRepository.save(userTechStack);
        return UserTechStackResponse.from(saved);
    }

    @Override
    public List<UserTechStackResponse> getTechStacks(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        List<UserTechStack1> userTechStacks = userTechStackRepository.findByUserWithTechStack(user);
        return userTechStacks.stream()
                .map(UserTechStackResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public UserTechStackResponse updateTechStack(String userEmail, Long userTechStackId, UserTechStackRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        UserTechStack1 userTechStack = userTechStackRepository.findByUserAndUserTechStackId(user, userTechStackId)
                .orElseThrow(() -> new NotFoundException("기술 스택 정보를 찾을 수 없습니다"));

        if (!userTechStack.getStack().getStackId().equals(request.getStackId())) {
            TechStackToDelete newTechStackToDelete = techStackRepository.findById(request.getStackId())
                    .orElseThrow(() -> new NotFoundException("기술 스택을 찾을 수 없습니다"));

            if (userTechStackRepository.existsByUserAndStack_StackId(user, request.getStackId())) {
                throw new IllegalArgumentException("이미 등록된 기술 스택입니다");
            }
            
            userTechStack.setStack(newTechStackToDelete);
        }

        userTechStack.setSkillLevel(request.getSkillLevel());
        UserTechStack1 saved = userTechStackRepository.save(userTechStack);
        return UserTechStackResponse.from(saved);
    }

    @Override
    public void deleteTechStack(String userEmail, Long userTechStackId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        UserTechStack1 userTechStack = userTechStackRepository.findByUserAndUserTechStackId(user, userTechStackId)
                .orElseThrow(() -> new NotFoundException("기술 스택 정보를 찾을 수 없습니다"));

        userTechStackRepository.delete(userTechStack);
    }
}