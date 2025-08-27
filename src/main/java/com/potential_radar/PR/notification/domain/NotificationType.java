package com.potential_radar.PR.notification.domain;

public enum NotificationType {
    // 프로젝트 초대
    INVITATION,

    // 소셜 기능
    LIKE,
    COMMENT,
    REPLY,

    // PM 알림
    APPLICATION,
    DEADLINE_IMMINENT,
    DEADLINE_TODAY,

    // 팀원 알림
    REVIEW_REMINDER,
    
    // 승인 알림
    APPLICATION_APPROVED
}
