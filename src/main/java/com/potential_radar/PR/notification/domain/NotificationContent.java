package com.potential_radar.PR.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable // 이 클래스가 내장 타입임을 명시
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자
@Getter // 필드 값에 접근하기 위한 Getter
public class NotificationContent {

    // 알림 내용을 저장할 필드
    // 컬럼 이름이 기본적으로 "content"가 되지만, 명시적으로 지정할 수도 있음
    @Column(nullable = false, length = 500) // NotNull 제약 조건 및 길이 제한
    private String content;

    // 모든 필드를 받는 생성자 (불변 객체를 위해 주로 사용)
    public NotificationContent(String content) {
        // 유효성 검사 로직 추가 가능
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Notification content cannot be null or empty.");
        }
        if (content.length() > 500) {
            throw new IllegalArgumentException("Notification content cannot exceed 500 characters.");
        }
        this.content = content;
    }
}
