package com.potential_radar.PR.invitation.controller;

import com.potential_radar.PR.invitation.dto.TeamInvitationDto;
import com.potential_radar.PR.invitation.service.TeamInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class TeamInvitationController {

    private final TeamInvitationService teamInvitationService;

    /**
     * 팀원 초대 보내기
     */
    @PostMapping("/send")
    public ResponseEntity<TeamInvitationDto.ApiResponse> sendInvitation(
            @RequestHeader("User-Id") Long userId,
            @RequestBody TeamInvitationDto.SendRequest request
    ) {
        log.info("팀원 초대 요청: 사용자 {} → 프로젝트 {}, 대상 {}", userId, request.getProjectId(), request.getInviteeId());
        
        TeamInvitationDto.ApiResponse response = teamInvitationService.sendInvitation(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 초대 응답하기 (수락/거절)
     */
    @PostMapping("/respond")
    public ResponseEntity<TeamInvitationDto.ApiResponse> respondToInvitation(
            @RequestHeader("User-Id") Long userId,
            @RequestBody TeamInvitationDto.ResponseRequest request
    ) {
        log.info("초대 응답: 사용자 {} → 초대 {}, 상태 {}", userId, request.getInvitationId(), request.getStatus());
        
        TeamInvitationDto.ApiResponse response = teamInvitationService.respondToInvitation(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 내가 받은 초대 목록 조회
     */
    @GetMapping("/received")
    public ResponseEntity<List<TeamInvitationDto.InvitationResponse>> getReceivedInvitations(
            @RequestHeader("User-Id") Long userId
    ) {
        log.info("받은 초대 목록 조회: 사용자 {}", userId);
        
        List<TeamInvitationDto.InvitationResponse> invitations = teamInvitationService.getReceivedInvitations(userId);
        return ResponseEntity.ok(invitations);
    }

    /**
     * 내가 보낸 초대 목록 조회
     */
    @GetMapping("/sent")
    public ResponseEntity<List<TeamInvitationDto.InvitationResponse>> getSentInvitations(
            @RequestHeader("User-Id") Long userId
    ) {
        log.info("보낸 초대 목록 조회: 사용자 {}", userId);
        
        List<TeamInvitationDto.InvitationResponse> invitations = teamInvitationService.getSentInvitations(userId);
        return ResponseEntity.ok(invitations);
    }
}