package com.potential_radar.PR.search.event;

import com.potential_radar.PR.user.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class UserUpdatedEvent {
    private final User user;
    private final User previousUser;
    private final LocalDateTime occurredAt;

    public UserUpdatedEvent(User user, User previousUser) {
        this.user = user;
        this.previousUser = previousUser;
        this.occurredAt = LocalDateTime.now();
    }

    public UserUpdatedEvent(User user) {
        this.user = user;
        this.previousUser = null;
        this.occurredAt = LocalDateTime.now();
    }
}