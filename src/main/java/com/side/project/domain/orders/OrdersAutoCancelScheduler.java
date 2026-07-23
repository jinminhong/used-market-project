package com.side.project.domain.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PAY_COMPLETED 상태로 {@code orders.auto-cancel.days}(기본 2일) 이상 발송되지 않은 주문을
 * 자동취소하기 위한 골격. 실제 조회/취소 로직(OrdersRepository.findStalePayCompleted 등)은
 * docs/ORDER_LIFECYCLE_GUIDE.md 설계에 맞춰 추후 구현한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrdersAutoCancelScheduler {

    @Value("${orders.auto-cancel.days:2}")
    private int autoCancelDays;

    @Scheduled(cron = "0 0 * * * *")
    public void cancelStalePayCompletedOrders() {
        log.debug("[OrdersAutoCancelScheduler] 자동취소 스케줄러 실행 - 기준일: {}일 (실제 취소 로직은 아직 미구현)", autoCancelDays);
    }
}
