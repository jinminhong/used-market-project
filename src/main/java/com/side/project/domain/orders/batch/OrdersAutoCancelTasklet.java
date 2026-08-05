package com.side.project.domain.orders.batch;

import com.side.project.domain.orders.OrderStatus;
import com.side.project.domain.orders.Orders;
import com.side.project.domain.orders.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrdersAutoCancelTasklet implements Tasklet{

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private final OrdersRepository ordersRepository;

    @Value("${orders.auto-cancel.days:2}")
    private int autoCancelDays;


    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LocalDateTime expiredDate = LocalDateTime.now(KOREA_ZONE).minusDays(autoCancelDays);

        List<Orders> expireReservedOrders = ordersRepository.findExpireReservedOrders(OrderStatus.RESERVED, expiredDate);

        for (Orders orders : expireReservedOrders) {
            orders.cancelExpireReservation();
        }

        log.info(
                "[OrdersAutoCancelTasklet] 자동취소 완료 - 기준시간: {}, 취소 건수: {}",
                expiredDate,
                expireReservedOrders.size()
        );

        return RepeatStatus.FINISHED;
    }
}
