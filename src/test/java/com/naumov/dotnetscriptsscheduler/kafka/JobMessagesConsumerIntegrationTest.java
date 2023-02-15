package com.naumov.dotnetscriptsscheduler.kafka;

import com.naumov.dotnetscriptsscheduler.AbstractIntegrationTest;
import com.naumov.dotnetscriptsscheduler.config.KafkaPropertyMapWrapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobFinishedMessage;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobMessage;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobStartedMessage;
import com.naumov.dotnetscriptsscheduler.model.*;
import com.naumov.dotnetscriptsscheduler.repository.JobsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Component
@SpringBootTest
@DirtiesContext
class JobMessagesConsumerIntegrationTest extends AbstractIntegrationTest {
    private static final long MESSAGE_WAITING_TIMEOUT_MS = 10000;
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
    @SpyBean
    private JobMessagesConsumer jobMessagesConsumerSpy;
    @MockBean
    private Reporter<JobMessage> jobMessageReporterMock;

    private UUID jobId;

    @BeforeEach
    public void setup() {
        assertEquals(0, jobsRepository.count());
        var startedProducerFactory = new DefaultKafkaProducerFactory<String, JobStartedMessage>(producerProps.toMap());
        var finishedProducerFactory = new DefaultKafkaProducerFactory<String, JobFinishedMessage>(producerProps.toMap());
        startedMessagesProducer = new KafkaTemplate<>(startedProducerFactory);
        finishedMessagesProducer = new KafkaTemplate<>(finishedProducerFactory);
    }

    @AfterEach
    public void teardown() {
        jobsRepository.deleteAll();
    }

    @Test
    void onJobStartedMessageForNotExistingJob() {
        // given
        jobId = UUID.randomUUID();
        JobStartedMessage jobTaskMessage = JobStartedMessage.builder().jobId(jobId).build();
        assertEquals(0, jobsRepository.count());

        // when
        startedMessagesProducer.send(runningTopic, jobId.toString(), jobTaskMessage);
        ArgumentCaptor<JobStartedMessage> messageCaptor = ArgumentCaptor.forClass(JobStartedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobStartedMessage(messageCaptor.capture(), any());
        verify(jobMessageReporterMock, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .report(any());

        // then
        JobStartedMessage message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
        assertEquals(0, jobsRepository.count());
    }

    @Test
    void onJobStartedMessageForPendingJob() {
        // given
        Job job = prepareJob(JobStatus.PENDING);
        jobId = job.getId();
        JobStartedMessage jobTaskMessage = JobStartedMessage.builder()
                .jobId(jobId)
                .build();

        // when
        startedMessagesProducer.send(runningTopic, jobId.toString(), jobTaskMessage);
        ArgumentCaptor<JobStartedMessage> messageCaptor = ArgumentCaptor.forClass(JobStartedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobStartedMessage(messageCaptor.capture(), any());
        verify(jobMessageReporterMock, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .report(any());

        // then
        assertTrue(jobsRepository.findById(jobId).isPresent());
        assertEquals(JobStatus.RUNNING, jobsRepository.findById(jobId).get().getStatus());
    }

    private Job prepareJob(JobStatus jobStatus) {
        JobRequestPayload payload = JobRequestPayload.builder()
                .script("script")
                .agentType("linux-amd64-dotnet-7")
                .build();

        JobRequest jobRequest = JobRequest.builder()
                .messageId("message-id")
                .senderId("sender-id")
                .payload(payload)
                .build();

        Job job = Job.builder()
                .status(jobStatus)
                .request(jobRequest)
                .build();

        return jobsRepository.saveAndFlush(job);
    }

    // TODO complete

    @Test
    void onJobFinishedMessage() {
    }
}