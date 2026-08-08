package com.side.project.domain.activitylog.event;

import com.side.project.domain.activitylog.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final ActivityLogService activityLogService;

    @Async("activityLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(UserActivityEvent event) {
        try {
            activityLogService.save(event);
        } catch (Exception e) {
            log.warn("활동 이력 저장에 실패했습니다. event={}", event, e);
        }
    }
}
