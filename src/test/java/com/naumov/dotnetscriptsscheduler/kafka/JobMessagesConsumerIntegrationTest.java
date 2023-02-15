package com.naumov.dotnetscriptsscheduler.kafka;

import com.naumov.dotnetscriptsscheduler.AbstractIntegrationTest;
import com.naumov.dotnetscriptsscheduler.config.KafkaPropertyMapWrapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.*;
import com.naumov.dotnetscriptsscheduler.model.*;
import com.naumov.dotnetscriptsscheduler.model.JobStatus;
import com.naumov.dotnetscriptsscheduler.repository.JobsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import java.util.Optional;
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
        UUID jobId = UUID.randomUUID();
        JobStartedMessage jobStartedMessage = prepareJobStartedMessage(jobId);

        // when
        startedMessagesProducer.send(runningTopic, jobId.toString(), jobStartedMessage);
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
        UUID jobId = job.getId();
        JobStartedMessage jobStartedMessage = prepareJobStartedMessage(jobId);

        // when
        startedMessagesProducer.send(runningTopic, jobId.toString(), jobStartedMessage);
        ArgumentCaptor<JobStartedMessage> messageCaptor = ArgumentCaptor.forClass(JobStartedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobStartedMessage(messageCaptor.capture(), any());
        verify(jobMessageReporterMock, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .report(any());

        // then
        assertTrue(jobsRepository.findById(jobId).isPresent());
        assertEquals(JobStatus.RUNNING, jobsRepository.findById(jobId).get().getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"FINISHED", "REJECTED"})
    void onJobStartedMessageForFinishedOrRejectedJob(JobStatus jobStatus) {
        // given
        Job job = prepareJob(jobStatus);
        UUID jobId = job.getId();
        JobStartedMessage jobStartedMessage = prepareJobStartedMessage(jobId);

        // when
        startedMessagesProducer.send(runningTopic, jobId.toString(), jobStartedMessage);
        ArgumentCaptor<JobStartedMessage> messageCaptor = ArgumentCaptor.forClass(JobStartedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobStartedMessage(messageCaptor.capture(), any());
        verify(jobMessageReporterMock, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .report(any());

        // then
        assertTrue(jobsRepository.findById(jobId).isPresent());
        assertEquals(jobStatus, jobsRepository.findById(jobId).get().getStatus());
    }

    private JobStartedMessage prepareJobStartedMessage(UUID jobId) {
        return JobStartedMessage.builder().jobId(jobId).build();
    }

    @Test
    void onJobFinishedMessageForNotExistingJob() {
        // given
        UUID jobId = UUID.randomUUID();
        JobFinishedMessage jobFinishedMessage = prepareFinishedMessage(jobId);
        assertEquals(0, jobsRepository.count());

        // when
        finishedMessagesProducer.send(finishedTopic, jobId.toString(), jobFinishedMessage);
        ArgumentCaptor<JobFinishedMessage> messageCaptor = ArgumentCaptor.forClass(JobFinishedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobFinishedMessage(messageCaptor.capture(), any());
        verify(jobMessageReporterMock, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .report(any());

        // then
        JobFinishedMessage message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
        assertEquals(0, jobsRepository.count());
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"PENDING", "RUNNING"})
    void onJobFinishedMessageForPendingOrRunningJob(JobStatus jobStatus) {
        // given
        Job job = prepareJob(jobStatus);
        UUID jobId = job.getId();
        JobFinishedMessage jobFinishedMessage = prepareFinishedMessage(jobId);

        // when
        finishedMessagesProducer.send(finishedTopic, jobId.toString(), jobFinishedMessage);
        ArgumentCaptor<JobFinishedMessage> messageCaptor = ArgumentCaptor.forClass(JobFinishedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobFinishedMessage(messageCaptor.capture(), any());
        verify(jobMessageReporterMock, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .report(any());

        // then
        Optional<Job> foundJobOptional = jobsRepository.findByIdFetchAll(jobId);
        assertTrue(foundJobOptional.isPresent());
        Job foundJob = foundJobOptional.get();
        assertEquals(JobStatus.FINISHED, foundJob.getStatus());
        JobResult foundJobResult = foundJob.getResult();
        assertNotNull(foundJobResult);
        assertEquals(JobResult.JobCompletionStatus.SUCCEEDED, foundJobResult.getFinishedWith());
        assertEquals("some-stdout", foundJobResult.getStdout());
        assertEquals("some-stderr", foundJobResult.getStderr());
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"FINISHED", "REJECTED"})
    void onJobFinishedMessageForFinishedOrRejectedJob(JobStatus jobStatus) {
        // given
        Job job = prepareJob(jobStatus);
        UUID jobId = job.getId();
        JobFinishedMessage jobFinishedMessage = prepareFinishedMessage(jobId);

        // when
        finishedMessagesProducer.send(finishedTopic, jobId.toString(), jobFinishedMessage);
        ArgumentCaptor<JobFinishedMessage> messageCaptor = ArgumentCaptor.forClass(JobFinishedMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobFinishedMessage(messageCaptor.capture(), any());
        verify(jobMessageReporterMock, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .report(any());

        // then
        Optional<Job> foundJobOptional = jobsRepository.findByIdFetchAll(jobId);
        assertTrue(foundJobOptional.isPresent());
        Job foundJob = foundJobOptional.get();
        assertEquals(jobStatus, foundJob.getStatus());
        assertNull(foundJob.getResult());
    }

    private JobFinishedMessage prepareFinishedMessage(UUID jobId) {
        ScriptResults scriptResults = ScriptResults.builder()
                .finishedWith(JobCompletionStatus.SUCCEEDED)
                .stdout("some-stdout")
                .stderr("some-stderr")
                .build();

        return JobFinishedMessage
                .builder()
                .jobId(jobId)
                .status(com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobStatus.ACCEPTED)
                .scriptResults(scriptResults)
                .build();
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
}