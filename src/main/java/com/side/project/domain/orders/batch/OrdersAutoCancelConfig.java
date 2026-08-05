package com.side.project.domain.orders.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OrdersAutoCancelConfig {

    @Bean
    public Step ordersAutoCancelStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     OrdersAutoCancelTasklet tasklet) {

        return new StepBuilder("ordersAutoCancelStep", jobRepository)
                .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Job ordersAutoCancelJob(JobRepository jobRepository,
                                   @Qualifier("ordersAutoCancelStep") Step ordersAutoCancelStep) {

        return new JobBuilder("ordersAutoCancelJob", jobRepository)
                .start(ordersAutoCancelStep).build();
    }
}
