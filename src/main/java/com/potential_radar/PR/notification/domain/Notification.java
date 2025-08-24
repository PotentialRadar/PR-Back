package com.potential_radar.PR.notification.domain;

import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = "receiver")
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    private String url;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User receiver;

    private Long invitationId;

    private LocalDateTime actionCreatedAt;

    @Builder.Default
    private Boolean isRead = false;


}
