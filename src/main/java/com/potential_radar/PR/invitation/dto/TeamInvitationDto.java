package com.potential_radar.PR.invitation.dto;

import com.potential_radar.PR.invitation.domain.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class TeamInvitationDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        private Long projectId;
        private Long inviteeId;
        private String message;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseRequest {
        private Long invitationId;
        private InvitationStatus status; // ACCEPTED or REJECTED
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvitationResponse {
        private Long invitationId;
        private Long projectId;
        private String projectTitle;
        private String projectDescription;
        private Long inviterId;
        private String inviterName;
        private Long inviteeId;
        private String inviteeName;
        private InvitationStatus status;
        private String message;
        private LocalDateTime createdAt;
        private LocalDateTime respondedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}