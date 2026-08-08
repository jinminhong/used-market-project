package com.side.project.domain.activitylog;

import com.side.project.domain.activitylog.event.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void save(UserActivityEvent event) {
        ActivityLog activityLog = new ActivityLog(event.getMemberId(), event.getItemId(), event.getActivityType());
        activityLogRepository.save(activityLog);
    }
}
