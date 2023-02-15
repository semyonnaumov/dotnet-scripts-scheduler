package com.naumov.dotnetscriptsscheduler.kafka;

import com.naumov.dotnetscriptsscheduler.dto.kafka.KafkaDtoMapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.*;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import com.naumov.dotnetscriptsscheduler.service.JobsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobMessagesConsumerTest {
    private JobsService jobsServiceMock;
    private final KafkaDtoMapper kafkaDtoMapper = new KafkaDtoMapper();
    private JobMessagesConsumer jobMessagesConsumer;

    @BeforeEach
    public void setup() {
        jobsServiceMock = mock(JobsService.class);
        jobMessagesConsumer = new JobMessagesConsumer(jobsServiceMock, kafkaDtoMapper, Optional.empty());
    }

    @Test
    void onJobStartedMessageRegular() {
        UUID jobId = UUID.randomUUID();
        JobStartedMessage jobStartedMessage = JobStartedMessage.builder().jobId(jobId).build();

        Acknowledgment ackMock = mock(Acknowledgment.class);
        jobMessagesConsumer.onJobStartedMessage(jobStartedMessage, ackMock);

        verify(jobsServiceMock, times(1)).updateStartedJob(eq((jobId)));
        verify(ackMock, times(1)).acknowledge();
    }

    @Test
    void onJobStartedMessageJobServiceThrowsException() {
        doThrow(RuntimeException.class).when(jobsServiceMock).updateStartedJob(any());

        UUID jobId = UUID.randomUUID();
        JobStartedMessage jobStartedMessage = JobStartedMessage.builder().jobId(jobId).build();

        Acknowledgment ackMock = mock(Acknowledgment.class);
        assertThrows(RuntimeException.class, () -> jobMessagesConsumer.onJobStartedMessage(jobStartedMessage, ackMock));

        verify(jobsServiceMock, times(1)).updateStartedJob(any());
        verify(ackMock, times(0)).acknowledge();
    }

    @Test
    void onJobFinishedMessageRegular() {
        UUID jobId = UUID.randomUUID();
        String stdout = "stdout";
        String stderr = "stderr";

        ScriptResults scriptResults = ScriptResults.builder()
                .finishedWith(JobCompletionStatus.SUCCEEDED)
                .stdout(stdout)
                .stderr(stderr)
                .build();

        JobFinishedMessage jobFinishedMessage = JobFinishedMessage.builder()
                .jobId(jobId)
                .status(JobStatus.ACCEPTED)
                .scriptResults(scriptResults)
                .build();

        Acknowledgment ackMock = mock(Acknowledgment.class);

        jobMessagesConsumer.onJobFinishedMessage(jobFinishedMessage, ackMock);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobsServiceMock, times(1)).updateFinishedJob(jobCaptor.capture());
        Job job = jobCaptor.getValue();
        assertNotNull(job);
        assertEquals(jobId, job.getId());
        assertEquals(com.naumov.dotnetscriptsscheduler.model.JobStatus.FINISHED, job.getStatus());
        JobResult jobResult = job.getResult();
        assertNotNull(jobResult);
        assertEquals(JobResult.JobCompletionStatus.SUCCEEDED, jobResult.getFinishedWith());
        assertEquals(stdout, jobResult.getStdout());
        assertEquals(stderr, jobResult.getStderr());

        verify(ackMock, times(1)).acknowledge();
    }

    @Test
    void onJobFinishedMessageJobServiceThrowsException() {
        doThrow(RuntimeException.class).when(jobsServiceMock).updateFinishedJob(any());

        JobFinishedMessage jobFinishedMessage = JobFinishedMessage.builder()
                .jobId(UUID.randomUUID())
                .status(JobStatus.ACCEPTED)
                .build();

        Acknowledgment ackMock = mock(Acknowledgment.class);

        assertThrows(RuntimeException.class, () -> jobMessagesConsumer.onJobFinishedMessage(jobFinishedMessage, ackMock));

        verify(jobsServiceMock, times(1)).updateFinishedJob(any());
        verify(ackMock, times(0)).acknowledge();
    }
}