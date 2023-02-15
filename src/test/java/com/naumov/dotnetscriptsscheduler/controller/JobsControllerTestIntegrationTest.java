package com.naumov.dotnetscriptsscheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naumov.dotnetscriptsscheduler.AbstractIntegrationTest;
import com.naumov.dotnetscriptsscheduler.config.KafkaPropertyMapWrapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobConfig;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobTaskMessage;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayloadConfig;
import com.naumov.dotnetscriptsscheduler.dto.rest.rs.*;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobPayloadConfig;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobStatus;
import com.naumov.dotnetscriptsscheduler.repository.JobsRepository;
import com.naumov.dotnetscriptsscheduler.service.WorkerTypesService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext
@AutoConfigureMockMvc
class JobsControllerTestIntegrationTest extends AbstractIntegrationTest {
    private static final String JOBS_PATH = "/jobs";
    private static final String REQUEST_PATH = "/request";
    private static final String STATUS_PATH = "/status";
    private static final String RESULT_PATH = "/result";
    private static final String NOT_EXISTING_WORKER_TYPE = "windows-amd64-dotnet-7";
    private static final String SCRIPT = "some script";
    private static final String NUGET_CONFIG_XML = "<config />";
    private static final String REQUEST_ID = "request-id";
    private static final String SENDER_ID = "sender-id";
    private static final String STDOUT = "some-stdout";
    private static final String STDERR = "some-stderr";
    private String defaultWorkerType;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    @Qualifier("commonConsumerProperties")
    private KafkaPropertyMapWrapper consumerProps;
    @Value("${scheduler.kafka.jobs-topic-prefix}")
    private String jobsTopicPrefix;
    @Value("${scheduler.kafka-admin.jobs-topics-partitions}")
    private Integer jobsTopicsPartitions;
    @Autowired
    private WorkerTypesService workerTypesService;
    @Autowired
    private JobsRepository jobsRepository;
    private KafkaMessageListenerContainer<String, JobTaskMessage> jobTaskMessageListenerContainer;
    private BlockingQueue<ConsumerRecord<String, JobTaskMessage>> consumedMessages;

    @BeforeEach
    public void setup() {
        // ensure worker types are correct
        defaultWorkerType = workerTypesService.getDefaultWorkerType();
        workerTypesService.workerExists(NOT_EXISTING_WORKER_TYPE);

        // db is empty
        assertEquals(0, jobsRepository.count());

        // setup kafka consumer
        assertTrue(workerTypesService.workerExists(defaultWorkerType));
        assertFalse(workerTypesService.workerExists(NOT_EXISTING_WORKER_TYPE));
        String jobsTaskTopic = jobsTopicPrefix + defaultWorkerType;

        consumedMessages = new LinkedBlockingQueue<>();

        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, JobTaskMessage.class.getName());

        var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps.toMap());

        jobTaskMessageListenerContainer = new KafkaMessageListenerContainer<>(consumerFactory, new ContainerProperties(jobsTaskTopic));
        jobTaskMessageListenerContainer.setupMessageListener((MessageListener<String, JobTaskMessage>) record -> consumedMessages.add(record));
        jobTaskMessageListenerContainer.start();

        ContainerTestUtils.waitForAssignment(jobTaskMessageListenerContainer, jobsTopicsPartitions);
    }

    @AfterEach
    public void teardown() {
        // clear the db
        jobsRepository.deleteAll();

        // setup kafka consumer
        jobTaskMessageListenerContainer.stop();
    }

    @Test
    void createJobJobNotExists() throws Exception {
        // given
        JobCreateRequest jobCreateRequest = prepareJobCreateRequest(REQUEST_ID,
                SENDER_ID, SCRIPT, defaultWorkerType, NUGET_CONFIG_XML);
        MockHttpServletRequestBuilder jobCreationRequest = post(JOBS_PATH)
                .content(objectMapper.writeValueAsString(jobCreateRequest))
                .contentType(MediaType.APPLICATION_JSON);

        // when
        String content = mvc.perform(jobCreationRequest)
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jobId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JobCreateResponse jobCreateResponse = objectMapper.readValue(content, JobCreateResponse.class);
        UUID jobId = jobCreateResponse.getJobId();

        // then
        // --- saved job is correct
        Optional<Job> jobOptional = jobsRepository.findByIdFetchAll(jobId);
        assertTrue(jobOptional.isPresent());
        Job job = jobOptional.get();
        JobRequest jobRequest = job.getRequest();
        assertNotNull(jobRequest);
        assertEquals(REQUEST_ID, jobRequest.getMessageId());
        assertEquals(SENDER_ID, jobRequest.getSenderId());
        com.naumov.dotnetscriptsscheduler.model.JobRequestPayload jobRequestModelPayload = jobRequest.getPayload();
        assertNotNull(jobRequestModelPayload);
        assertEquals(SCRIPT, jobRequestModelPayload.getScript());
        assertEquals(defaultWorkerType, jobRequestModelPayload.getAgentType());
        JobPayloadConfig jobPayloadConfig = jobRequestModelPayload.getJobPayloadConfig();
        assertNotNull(jobPayloadConfig);
        assertEquals(NUGET_CONFIG_XML, jobPayloadConfig.getNugetConfigXml());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertNull(job.getResult());
        assertNotNull(job.getCreationOffsetDateTime());

        // --- message is sent and correct
        ConsumerRecord<String, JobTaskMessage> receivedMessage = consumedMessages.poll(10, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertNotNull(receivedMessage.value());
        assertNotNull(receivedMessage.value().getJobId());
        assertEquals(jobId.toString(), receivedMessage.key());
        JobTaskMessage message = receivedMessage.value();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
        assertEquals(SCRIPT, message.getScript());
        JobConfig jobConfig = message.getJobConfig();
        assertNotNull(jobConfig);
        assertEquals(NUGET_CONFIG_XML, jobConfig.getNugetConfigXml());
    }

    @Test
    void createJobJobExists() throws Exception {
        // given
        // --- job is in the db
        Job savedJob = prepareAndSaveJob(JobStatus.PENDING,
                REQUEST_ID, SENDER_ID, SCRIPT, defaultWorkerType, NUGET_CONFIG_XML, false);

        assertNotNull(savedJob);
        assertTrue(jobsRepository.existsById(savedJob.getId()));
        // --- request contains the same requestId
        JobCreateRequest jobCreateRequest = prepareJobCreateRequest(REQUEST_ID,
                "other-sender-id", "other-script", defaultWorkerType, "<other-config />");

        MockHttpServletRequestBuilder jobCreationRequest = post(JOBS_PATH)
                .content(objectMapper.writeValueAsString(jobCreateRequest))
                .contentType(MediaType.APPLICATION_JSON);

        // when
        String content = mvc.perform(jobCreationRequest)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jobId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JobCreateResponse jobCreateResponse = objectMapper.readValue(content, JobCreateResponse.class);
        UUID jobId = jobCreateResponse.getJobId();

        // then
        // --- saved job is intact
        Optional<Job> jobOptional = jobsRepository.findByIdFetchAll(jobId);
        assertTrue(jobOptional.isPresent());
        Job job = jobOptional.get();
        JobRequest jobRequest = job.getRequest();
        assertNotNull(jobRequest);
        assertEquals(REQUEST_ID, jobRequest.getMessageId());
        assertEquals(SENDER_ID, jobRequest.getSenderId());
        com.naumov.dotnetscriptsscheduler.model.JobRequestPayload jobRequestModelPayload = jobRequest.getPayload();
        assertNotNull(jobRequestModelPayload);
        assertEquals(SCRIPT, jobRequestModelPayload.getScript());
        assertEquals(defaultWorkerType, jobRequestModelPayload.getAgentType());
        JobPayloadConfig jobPayloadConfig = jobRequestModelPayload.getJobPayloadConfig();
        assertNotNull(jobPayloadConfig);
        assertEquals(NUGET_CONFIG_XML, jobPayloadConfig.getNugetConfigXml());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertNull(job.getResult());
        assertNotNull(job.getCreationOffsetDateTime());

        // --- message is not sent
        assertNull(consumedMessages.poll(10, TimeUnit.SECONDS));
    }

    @Test
    void createJobJobWrongAgentType() throws Exception {
        // given
        JobCreateRequest jobCreateRequest = prepareJobCreateRequest(REQUEST_ID,
                SENDER_ID, SCRIPT, NOT_EXISTING_WORKER_TYPE, NUGET_CONFIG_XML);

        MockHttpServletRequestBuilder jobCreationRequest = post(JOBS_PATH)
                .content(objectMapper.writeValueAsString(jobCreateRequest))
                .contentType(MediaType.APPLICATION_JSON);

        // when
        mvc.perform(jobCreationRequest)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", notNullValue()));

        // then
        // --- job not saved
        assertEquals(0, jobsRepository.count());
        // --- message is not sent
        assertNull(consumedMessages.poll(10, TimeUnit.SECONDS));
    }

    private JobCreateRequest prepareJobCreateRequest(String requestId,
                                                     String senderId,
                                                     String script,
                                                     String workerType,
                                                     String nugetConfigXml) {
        JobRequestPayload jobRequestPayload = JobRequestPayload.builder()
                .script(script)
                .agentType(workerType)
                .jobConfig(JobRequestPayloadConfig.builder().nugetConfigXml(nugetConfigXml).build())
                .build();

        return JobCreateRequest.builder()
                .requestId(requestId)
                .senderId(senderId)
                .payload(jobRequestPayload)
                .build();
    }

    private Job prepareAndSaveJob(JobStatus jobStatus,
                                  String requestId,
                                  String senderId,
                                  String script,
                                  String workerType,
                                  String nugetConfigXml,
                                  boolean withResult) {
        com.naumov.dotnetscriptsscheduler.model.JobRequestPayload payload =
                com.naumov.dotnetscriptsscheduler.model.JobRequestPayload.builder()
                        .script(script)
                        .agentType(workerType)
                        .jobPayloadConfig(JobPayloadConfig.builder().nugetConfigXml(nugetConfigXml).build())
                        .build();

        JobRequest jobRequest = JobRequest.builder()
                .messageId(requestId)
                .senderId(senderId)
                .payload(payload)
                .build();

        Job job = Job.builder()
                .status(jobStatus)
                .request(jobRequest)
                .build();

        if (withResult) {
            com.naumov.dotnetscriptsscheduler.model.JobResult jobResult =
                    com.naumov.dotnetscriptsscheduler.model.JobResult.builder()
                            .finishedWith(com.naumov.dotnetscriptsscheduler.model.JobResult.JobCompletionStatus.SUCCEEDED)
                            .stdout(STDOUT)
                            .stderr(STDERR)
                            .build();

            job.setResult(jobResult);
        }

        return jobsRepository.saveAndFlush(job);
    }

    @Test
    void getJobJobExists() throws Exception {
        // given
        Job savedJob = prepareAndSaveJob(JobStatus.PENDING,
                REQUEST_ID, SENDER_ID, SCRIPT, defaultWorkerType, NUGET_CONFIG_XML, true);

        assertNotNull(savedJob);
        UUID jobId = savedJob.getId();
        assertTrue(jobsRepository.existsById(jobId));

        // when
        String responseString = mvc.perform(get(JOBS_PATH + "/" + jobId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        JobGetResponse jobGetResponse = objectMapper.readValue(responseString, JobGetResponse.class);
        assertNotNull(jobGetResponse);
        assertEquals(jobId, jobGetResponse.getJobId());
        JobCreateRequest jobCreateRequest = jobGetResponse.getRequest();
        assertNotNull(jobCreateRequest);
        assertEquals(REQUEST_ID, jobCreateRequest.getRequestId());
        assertEquals(SENDER_ID, jobCreateRequest.getSenderId());
        JobRequestPayload jobRequestPayload = jobCreateRequest.getPayload();
        assertNotNull(jobRequestPayload);
        assertEquals(SCRIPT, jobRequestPayload.getScript());
        assertEquals(defaultWorkerType, jobRequestPayload.getAgentType());
        JobRequestPayloadConfig jobPayloadConfig = jobRequestPayload.getJobConfig();
        assertNotNull(jobPayloadConfig);
        assertEquals(NUGET_CONFIG_XML, jobPayloadConfig.getNugetConfigXml());
        assertEquals(com.naumov.dotnetscriptsscheduler.dto.rest.rs.JobStatus.PENDING, jobGetResponse.getStatus());
        JobResult result = jobGetResponse.getResult();
        assertNotNull(result);
        assertEquals(JobCompletionStatus.SUCCEEDED, result.getFinishedWith());
        assertEquals(STDOUT, result.getStdout());
        assertEquals(STDERR, result.getStderr());
    }

    @Test
    void getJobJobNotExists() throws Exception {
        // given
        UUID jobId = UUID.randomUUID();
        assertFalse(jobsRepository.existsById(jobId));

        // when
        mvc.perform(get(JOBS_PATH + "/" + jobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJobRequestJobExists() throws Exception {
        // given
        Job savedJob = prepareAndSaveJob(JobStatus.PENDING,
                REQUEST_ID, SENDER_ID, SCRIPT, defaultWorkerType, NUGET_CONFIG_XML, false);

        assertNotNull(savedJob);
        UUID jobId = savedJob.getId();
        assertTrue(jobsRepository.existsById(jobId));

        // when
        String responseString = mvc.perform(get(JOBS_PATH + "/" + jobId + REQUEST_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        JobGetRequestResponse jobGetRequestResponse = objectMapper.readValue(responseString, JobGetRequestResponse.class);
        assertNotNull(jobGetRequestResponse);
        assertEquals(jobId, jobGetRequestResponse.getJobId());
        JobCreateRequest jobCreateRequest = jobGetRequestResponse.getRequest();
        assertNotNull(jobCreateRequest);
        assertEquals(REQUEST_ID, jobCreateRequest.getRequestId());
        assertEquals(SENDER_ID, jobCreateRequest.getSenderId());
        JobRequestPayload jobRequestPayload = jobCreateRequest.getPayload();
        assertNotNull(jobRequestPayload);
        assertEquals(SCRIPT, jobRequestPayload.getScript());
        assertEquals(defaultWorkerType, jobRequestPayload.getAgentType());
        JobRequestPayloadConfig jobPayloadConfig = jobRequestPayload.getJobConfig();
        assertNotNull(jobPayloadConfig);
        assertEquals(NUGET_CONFIG_XML, jobPayloadConfig.getNugetConfigXml());
    }

    @Test
    void getJobRequestJobNotExists() throws Exception {
        // given
        UUID jobId = UUID.randomUUID();
        assertFalse(jobsRepository.existsById(jobId));

        // when
        mvc.perform(get(JOBS_PATH + "/" + jobId + REQUEST_PATH))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJobStatusJobExists() throws Exception {
        // given
        Job savedJob = prepareAndSaveJob(JobStatus.PENDING,
                REQUEST_ID, SENDER_ID, SCRIPT, defaultWorkerType, NUGET_CONFIG_XML, false);

        assertNotNull(savedJob);
        UUID jobId = savedJob.getId();
        assertTrue(jobsRepository.existsById(jobId));

        // when
        String responseString = mvc.perform(get(JOBS_PATH + "/" + jobId + STATUS_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        JobGetStatusResponse jobGetStatusResponse = objectMapper.readValue(responseString, JobGetStatusResponse.class);
        assertNotNull(jobGetStatusResponse);
        assertEquals(jobId, jobGetStatusResponse.getJobId());
        assertEquals(com.naumov.dotnetscriptsscheduler.dto.rest.rs.JobStatus.PENDING, jobGetStatusResponse.getStatus());
    }

    @Test
    void getJobStatusJobNotExists() throws Exception {
        // given
        UUID jobId = UUID.randomUUID();
        assertFalse(jobsRepository.existsById(jobId));

        // when
        mvc.perform(get(JOBS_PATH + "/" + jobId + STATUS_PATH))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJobResultJobExists() throws Exception {
        // given
        Job savedJob = prepareAndSaveJob(JobStatus.PENDING,
                REQUEST_ID, SENDER_ID, SCRIPT, defaultWorkerType, NUGET_CONFIG_XML, true);

        assertNotNull(savedJob);
        UUID jobId = savedJob.getId();
        assertTrue(jobsRepository.existsById(jobId));

        // when
        String responseString = mvc.perform(get(JOBS_PATH + "/" + jobId + RESULT_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        JobGetResultResponse jobGetResultResponse = objectMapper.readValue(responseString, JobGetResultResponse.class);
        assertNotNull(jobGetResultResponse);
        assertEquals(jobId, jobGetResultResponse.getJobId());
        JobResult result = jobGetResultResponse.getResult();
        assertNotNull(result);
        assertEquals(JobCompletionStatus.SUCCEEDED, result.getFinishedWith());
        assertEquals(STDOUT, result.getStdout());
        assertEquals(STDERR, result.getStderr());
    }

    @Test
    void getJobResultJobNotExists() throws Exception {
        // given
        UUID jobId = UUID.randomUUID();
        assertFalse(jobsRepository.existsById(jobId));

        // when
        mvc.perform(get(JOBS_PATH + "/" + jobId + RESULT_PATH))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteJobJobExists() throws Exception {
        // given
        Job savedJob = prepareAndSaveJob(JobStatus.PENDING,
                REQUEST_ID, SENDER_ID, SCRIPT, defaultWorkerType, NUGET_CONFIG_XML, false);

        assertNotNull(savedJob);
        UUID jobId = savedJob.getId();
        assertTrue(jobsRepository.existsById(jobId));

        // when
        mvc.perform(delete(JOBS_PATH + "/" + jobId))
                .andExpect(status().isOk());
    }

    @Test
    void deleteJobJobNotExists() throws Exception {
        // given
        UUID jobId = UUID.randomUUID();
        assertFalse(jobsRepository.existsById(jobId));

        // when
        mvc.perform(delete(JOBS_PATH + "/" + jobId))
                .andExpect(status().isNotFound());
    }
}