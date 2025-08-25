package com.potential_radar.PR.notification.dto;

import com.potential_radar.PR.notification.domain.Notification;
import com.potential_radar.PR.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String content;
    private String url;
    private NotificationType notificationType;
    private Long invitationId;
    private LocalDateTime actionCreatedAt;
    private Boolean isRead;

    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .url(notification.getUrl())
                .notificationType(notification.getNotificationType())
                .invitationId(notification.getInvitationId())
                .actionCreatedAt(notification.getActionCreatedAt())
                .isRead(notification.getIsRead())
                .build();
    }
}
