package com.side.project.domain.orders.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class OrdersAutoCancelScheduler {

    @Value("${orders.auto-cancel.days:2}")
    private int autoCancelDays;

    private final JobOperator jobOperator;
    private final Job ordersAutoCancelJob;

    public OrdersAutoCancelScheduler(
            JobOperator jobOperator,
            @Qualifier("ordersAutoCancelJob")
            Job ordersAutoCancelJob
    ) {
        this.jobOperator = jobOperator;
        this.ordersAutoCancelJob = ordersAutoCancelJob;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cancelStaleReservedOrders() {
        JobParameters jobParameters = new JobParametersBuilder()
                                    .addLocalDateTime("runAt", LocalDateTime.now()).toJobParameters();

        try {
            JobExecution execution = jobOperator.start(ordersAutoCancelJob, jobParameters);
            log.debug( "[OrdersAutoCancelScheduler] Batch 실행 - executionId: {}, status: {}",
                    execution.getId(),
                    execution.getStatus());
        } catch (Exception e) {
            log.error("[OrdersAutoCancelScheduler] Batch 실행 실패",e);
        }
    }
}
