package com.naumov.dotnetscriptsscheduler.kafka;

import com.naumov.dotnetscriptsscheduler.dto.kafka.KafkaDtoMapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobFinishedMessage;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobMessage;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobStartedMessage;
import com.naumov.dotnetscriptsscheduler.service.JobService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JobMessagesConsumer {
    private static final Logger LOGGER = LogManager.getLogger(JobMessagesConsumer.class);
    private final JobService jobService;
    private final KafkaDtoMapper kafkaDtoMapper;
    private final Optional<Reporter<JobMessage>> messageProcessedReporter;

    @Autowired
    public JobMessagesConsumer(JobService jobService,
                               KafkaDtoMapper kafkaDtoMapper,
                               Optional<Reporter<JobMessage>> messageProcessedReporter) {
        this.jobService = jobService;
        this.kafkaDtoMapper = kafkaDtoMapper;
        this.messageProcessedReporter = messageProcessedReporter;
    }

    @KafkaListener(
            topics = "${scheduler.kafka.running-topic-name}",
            containerFactory = "jobStartedMessagesKafkaListenerContainerFactory",
            errorHandler = "kafkaListenerPayloadValidationErrorHandler"
    )
    public void onJobStartedMessage(@Payload @Valid JobStartedMessage jobStartedMessage, Acknowledgment ack) {
        UUID jobId = jobStartedMessage.getJobId();
        LOGGER.info("Received job {} started message", jobId);
        try {
            jobService.updateStartedJob(jobId);
            LOGGER.info("Processed job {} started message", jobId);
            onJobMessageProcessed(jobStartedMessage);
            ack.acknowledge();
        } catch (RuntimeException e) {
            LOGGER.error("Failed to process job {} started message", jobStartedMessage);
            throw e;
        }
    }

    @KafkaListener(
            topics = "${scheduler.kafka.finished-topic-name}",
            containerFactory = "jobFinishedMessagesKafkaListenerContainerFactory",
            errorHandler = "kafkaListenerPayloadValidationErrorHandler"
    )
    public void onJobFinishedMessage(@Payload @Valid JobFinishedMessage jobFinishedMessage, Acknowledgment ack) {
        UUID jobId = jobFinishedMessage.getJobId();
        LOGGER.info("Received job {} finished message", jobId);
        try {
            jobService.updateFinishedJob(kafkaDtoMapper.fromJobFinishedMessage(jobFinishedMessage));
            LOGGER.info("Processed job {} finished message", jobId);
            onJobMessageProcessed(jobFinishedMessage);
            ack.acknowledge();
        } catch (RuntimeException e) {
            LOGGER.error("Failed to process job {} finished message", jobFinishedMessage);
            throw e;
        }
    }

    private void onJobMessageProcessed(JobMessage jobMessage) {
        messageProcessedReporter.ifPresent(r -> r.report(jobMessage));
    }
}
