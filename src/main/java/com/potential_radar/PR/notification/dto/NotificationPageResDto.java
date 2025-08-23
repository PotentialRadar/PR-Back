package com.potential_radar.PR.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageResDto {
    private List<NotificationDto> notifications; // 현재 페이지의 알림 리스트
    private boolean hasNext; // 다음 페이지가 있는지 여부
    private Long lastId; // 마지막 알림 ID
    private Long totalUnreadCount; // 읽지 않은 총 알림 개수
}
