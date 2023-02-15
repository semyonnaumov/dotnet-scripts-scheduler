package com.naumov.dotnetscriptsscheduler.kafka;

import com.naumov.dotnetscriptsscheduler.AbstractIntegrationTest;
import com.naumov.dotnetscriptsscheduler.config.KafkaPropertyMapWrapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobFinishedMessage;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobStartedMessage;
import com.naumov.dotnetscriptsscheduler.model.*;
import com.naumov.dotnetscriptsscheduler.repository.JobsRepository;
import com.naumov.dotnetscriptsscheduler.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.AfterTransaction;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Component
@SpringBootTest
@DirtiesContext
class JobMessagesConsumerIntegrationTest extends AbstractIntegrationTest {
    @Value("${scheduler.kafka.running-topic-name}")
    private String runningTopic;
    @Value("${scheduler.kafka.finished-topic-name}")
    private String finishedTopic;
    @Autowired
    @Qualifier("commonProducerProperties")
    private KafkaPropertyMapWrapper producerProps;
    private KafkaTemplate<String, JobStartedMessage> startedMessagesProducer;
    private KafkaTemplate<String, JobFinishedMessage> finishedMessagesProducer;
    @Autowired
    private JobsRepository jobsRepository;
    @Autowired
    private JobService jobService;
    @SpyBean
    private JobMessagesConsumer jobMessagesConsumerSpy;

    private UUID jobId;

    @BeforeEach
    public void setup() {
        var startedProducerFactory = new DefaultKafkaProducerFactory<String, JobStartedMessage>(producerProps.toMap());
        var finishedProducerFactory = new DefaultKafkaProducerFactory<String, JobFinishedMessage>(producerProps.toMap());
        startedMessagesProducer = new KafkaTemplate<>(startedProducerFactory);
        finishedMessagesProducer = new KafkaTemplate<>(finishedProducerFactory);
    }

    @Test
    void onJobStartedMessageForNotExistingJob() {
        jobId = UUID.randomUUID();
        JobStartedMessage jobTaskMessage = JobStartedMessage.builder().jobId(jobId).build();
        assertTrue(jobsRepository.findAll().isEmpty());

        startedMessagesProducer.send(runningTopic, jobId.toString(), jobTaskMessage);
        ArgumentCaptor<JobStartedMessage> messageCaptor = ArgumentCaptor.forClass(JobStartedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(10000).times(1)).onJobStartedMessage(messageCaptor.capture(), any());

        JobStartedMessage message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
        assertTrue(jobsRepository.findAll().isEmpty());
    }

    @Test
    void onJobStartedMessageForPendingJob() throws InterruptedException {
        Job job = prepareJob();
        jobId = job.getId();
        assertTrue(jobsRepository.existsById(jobId));
        JobStartedMessage jobTaskMessage = JobStartedMessage.builder().jobId(jobId).build();

        startedMessagesProducer.send(runningTopic, jobId.toString(), jobTaskMessage);
        Acknowledgment ackMock = mock(Acknowledgment.class);
        ArgumentCaptor<JobStartedMessage> messageCaptor = ArgumentCaptor.forClass(JobStartedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(10000).times(1)).onJobStartedMessage(messageCaptor.capture(), any());
        // TODO wait for ack to ack() here instead of thread.sleep?

        Thread.sleep(10000);

        Optional<JobStatus> jobStatusByJobId = jobService.findJobStatusByJobId(jobId);
        assertTrue(jobStatusByJobId.isPresent());
        assertEquals(JobStatus.RUNNING, jobStatusByJobId.get());
    }

    @AfterTransaction
    public void afterTransaction() {
        Optional<Job> job = jobService.findJob(jobId);
        if (job.isPresent()) {
            System.out.println("After transaction");
            System.out.println(job.get());
        }
    }

    private Job prepareJob() {
        JobRequestPayload payload = JobRequestPayload.builder()
                .script("script")
                .agentType("linux-amd64-dotnet-7")
                .build();

        JobRequest jobRequest = JobRequest.builder()
                .messageId("message-id")
                .senderId("sender-id")
                .payload(payload)
                .build();

        return jobService.createOrGetJob(jobRequest).getJob();
    }

    // TODO complete

    @Test
    void onJobFinishedMessage() {
    }
}