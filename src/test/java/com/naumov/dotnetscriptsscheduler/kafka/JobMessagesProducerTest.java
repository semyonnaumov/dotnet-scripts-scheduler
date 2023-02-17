package com.naumov.dotnetscriptsscheduler.kafka;

import com.naumov.dotnetscriptsscheduler.config.props.SchedulerKafkaProperties;
import com.naumov.dotnetscriptsscheduler.dto.kafka.KafkaDtoMapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobTaskMessage;
import com.naumov.dotnetscriptsscheduler.exception.BadInputException;
import com.naumov.dotnetscriptsscheduler.exception.JobMessagesProducerException;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobPayloadConfig;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.service.WorkerTypesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JobMessagesProducerTest {
    private final KafkaDtoMapper kafkaDtoMapper = new KafkaDtoMapper();
    private WorkerTypesService workerTypesServiceMock;
    private KafkaTemplate<String, JobTaskMessage> kafkaTemplateMock;
    private SchedulerKafkaProperties schedulerKafkaPropertiesMock;
    private JobMessagesProducer jobMessagesProducer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        workerTypesServiceMock = mock(WorkerTypesService.class);
        kafkaTemplateMock = (KafkaTemplate<String, JobTaskMessage>) mock(KafkaTemplate.class);
        schedulerKafkaPropertiesMock = mock(SchedulerKafkaProperties.class);

        jobMessagesProducer = new JobMessagesProducer(
                schedulerKafkaPropertiesMock,
                workerTypesServiceMock,
                kafkaTemplateMock,
                kafkaDtoMapper
        );
    }

    @Test
    void sendJobTaskMessageAsyncRegular() {
        UUID jobId = UUID.randomUUID();
        String prefix = "some-prefix-";
        String workerType = "some-worker-type";
        String script = "some script";
        String nugetConfigXml = "<config />";

        when(schedulerKafkaPropertiesMock.getJobsTopicPrefix()).thenReturn(prefix);
        when(workerTypesServiceMock.workerExists(workerType)).thenReturn(true);
        when(kafkaTemplateMock.send(anyString(), anyString(), any(JobTaskMessage.class)))
                .thenReturn(new CompletableFuture<>());

        JobRequestPayload jobRequestPayload = JobRequestPayload.builder()
                .script(script)
                .agentType(workerType)
                .jobPayloadConfig(JobPayloadConfig.builder().nugetConfigXml(nugetConfigXml).build())
                .build();

        Job job = Job.builder()
                .id(jobId)
                .request(JobRequest.builder().payload(jobRequestPayload).build())
                .build();

        jobMessagesProducer.sendJobTaskMessageAsync(job);

        verify(workerTypesServiceMock, times(1)).workerExists(eq(workerType));
        verify(schedulerKafkaPropertiesMock, times(1)).getJobsTopicPrefix();
        ArgumentCaptor<JobTaskMessage> argumentCaptor = ArgumentCaptor.forClass(JobTaskMessage.class);
        verify(kafkaTemplateMock, times(1))
                .send(eq(prefix + workerType), eq(jobId.toString()), argumentCaptor.capture());

        JobTaskMessage message = argumentCaptor.getValue();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
        assertEquals(script, message.getScript());
        assertNotNull(message.getJobConfig());
        assertEquals(nugetConfigXml, message.getJobConfig().getNugetConfigXml());
    }

    @Test
    void sendJobTaskMessageAsyncMultipleAgentTypes() {
        UUID jobId = UUID.randomUUID();
        String prefix = "some-prefix-";
        String workerType1 = "worker-type-1";
        String workerType2 = "worker-type-2";

        when(schedulerKafkaPropertiesMock.getJobsTopicPrefix()).thenReturn(prefix);
        when(workerTypesServiceMock.workerExists(workerType1)).thenReturn(true);
        when(workerTypesServiceMock.workerExists(workerType2)).thenReturn(true);
        when(kafkaTemplateMock.send(anyString(), anyString(), any(JobTaskMessage.class)))
                .thenReturn(new CompletableFuture<>());

        JobRequestPayload jobRequestPayload = JobRequestPayload.builder()
                .script("some script")
                .agentType(workerType1)
                .jobPayloadConfig(JobPayloadConfig.builder().build())
                .build();

        Job job = Job.builder()
                .id(jobId)
                .request(JobRequest.builder().payload(jobRequestPayload).build())
                .build();

        jobMessagesProducer.sendJobTaskMessageAsync(job);

        job.getRequest().getPayload().setAgentType(workerType2);

        jobMessagesProducer.sendJobTaskMessageAsync(job);

        verify(workerTypesServiceMock, times(1)).workerExists(eq(workerType1));
        verify(workerTypesServiceMock, times(1)).workerExists(eq(workerType2));
        verify(schedulerKafkaPropertiesMock, times(2)).getJobsTopicPrefix();
        verify(kafkaTemplateMock, times(1)).send(eq(prefix + workerType1), anyString(), any());
        verify(kafkaTemplateMock, times(1)).send(eq(prefix + workerType2), anyString(), any());
    }

    @Test
    void sendJobTaskMessageAsyncWrongAgentType() {
        UUID jobId = UUID.randomUUID();

        when(schedulerKafkaPropertiesMock.getJobsTopicPrefix()).thenReturn("some-prefix-");
        when(workerTypesServiceMock.workerExists(anyString())).thenReturn(false);
        when(kafkaTemplateMock.send(anyString(), anyString(), any(JobTaskMessage.class)))
                .thenReturn(new CompletableFuture<>());

        JobRequestPayload jobRequestPayload = JobRequestPayload.builder()
                .script("some script")
                .agentType("some-worker-type")
                .jobPayloadConfig(JobPayloadConfig.builder().build())
                .build();

        Job job = Job.builder()
                .id(jobId)
                .request(JobRequest.builder().payload(jobRequestPayload).build())
                .build();

        assertThrows(BadInputException.class, () -> jobMessagesProducer.sendJobTaskMessageAsync(job));
        verify(workerTypesServiceMock, times(1)).workerExists(anyString());
        verify(schedulerKafkaPropertiesMock, times(0)).getJobsTopicPrefix();
        verify(kafkaTemplateMock, times(0)).send(anyString(), anyString(), any());
    }

    @Test
    void sendJobTaskMessageAsyncNoAgentType() {
        UUID jobId = UUID.randomUUID();

        when(schedulerKafkaPropertiesMock.getJobsTopicPrefix()).thenReturn("some-prefix-");
        when(workerTypesServiceMock.workerExists(anyString())).thenReturn(true);
        when(kafkaTemplateMock.send(anyString(), anyString(), any(JobTaskMessage.class)))
                .thenReturn(new CompletableFuture<>());

        JobRequestPayload jobRequestPayload = JobRequestPayload.builder()
                .script("some script")
                .jobPayloadConfig(JobPayloadConfig.builder().build())
                .build();

        Job job = Job.builder()
                .id(jobId)
                .request(JobRequest.builder().payload(jobRequestPayload).build())
                .build();

        assertThrows(JobMessagesProducerException.class, () -> jobMessagesProducer.sendJobTaskMessageAsync(job));
        verify(workerTypesServiceMock, times(0)).workerExists(anyString());
        verify(schedulerKafkaPropertiesMock, times(0)).getJobsTopicPrefix();
        verify(kafkaTemplateMock, times(0)).send(anyString(), anyString(), any());
    }
}