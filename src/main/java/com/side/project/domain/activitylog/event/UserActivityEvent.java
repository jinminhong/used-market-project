package com.side.project.domain.activitylog.event;

import com.side.project.domain.activitylog.ActivityType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserActivityEvent {

    private final Long memberId;
    private final Long itemId;
    private final ActivityType activityType;
    private final LocalDateTime occurredAt;

    public UserActivityEvent(Long memberId, Long itemId, ActivityType activityType) {
        this.memberId = memberId;
        this.itemId = itemId;
        this.activityType = activityType;
        this.occurredAt = LocalDateTime.now();
    }
}
