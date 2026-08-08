package com.side.project.domain.activitylog;

import com.side.project.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@NoArgsConstructor
public class ActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = true)
    private Long memberId;

    private Long itemId;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    public ActivityLog(Long memberId, Long itemId, ActivityType activityType) {
        this.memberId = memberId;
        this.itemId = itemId;
        this.activityType = activityType;
    }
}
