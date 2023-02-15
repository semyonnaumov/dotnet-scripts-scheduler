package com.naumov.dotnetscriptsscheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naumov.dotnetscriptsscheduler.AbstractIntegrationTest;
import com.naumov.dotnetscriptsscheduler.config.KafkaPropertyMapWrapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobConfig;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobTaskMessage;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayloadConfig;
import com.naumov.dotnetscriptsscheduler.dto.rest.rs.JobCreateResponse;
import com.naumov.dotnetscriptsscheduler.kafka.JobMessagesProducer;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext
@AutoConfigureMockMvc
class JobsControllerTestIntegrationTest extends AbstractIntegrationTest {
    private final String existingWorkerType = "linux-amd64-dotnet-7";
    private final String notExistingWorkerType = "windows-amd64-dotnet-7";
    private final String script = "some script";
    private final String nugetConfigXml = "<config />";
    private final String requestId = "request-id";
    private final String senderId = "sender-id";
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
    private JobMessagesProducer jobMessagesProducer;
    @Autowired
    private JobsRepository jobsRepository;
    private KafkaMessageListenerContainer<String, JobTaskMessage> jobTaskMessageListenerContainer;
    private BlockingQueue<ConsumerRecord<String, JobTaskMessage>> consumedMessages;

    @BeforeEach
    public void setup() {
        // db is empty
        assertEquals(0, jobsRepository.count());

        // setup kafka consumer
        assertTrue(workerTypesService.workerExists(existingWorkerType));
        assertFalse(workerTypesService.workerExists(notExistingWorkerType));
        String jobsTaskTopic = jobsTopicPrefix + existingWorkerType;

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
        JobCreateRequest jobCreateRequest = prepareJobCreateRequest(requestId,
                senderId, script, existingWorkerType, nugetConfigXml);
        MockHttpServletRequestBuilder jobCreationRequest = post("/jobs")
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
        assertEquals(requestId, jobRequest.getMessageId());
        assertEquals(senderId, jobRequest.getSenderId());
        com.naumov.dotnetscriptsscheduler.model.JobRequestPayload jobRequestModelPayload = jobRequest.getPayload();
        assertNotNull(jobRequestModelPayload);
        assertEquals(script, jobRequestModelPayload.getScript());
        assertEquals(existingWorkerType, jobRequestModelPayload.getAgentType());
        JobPayloadConfig jobPayloadConfig = jobRequestModelPayload.getJobPayloadConfig();
        assertNotNull(jobPayloadConfig);
        assertEquals(nugetConfigXml, jobPayloadConfig.getNugetConfigXml());
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
        assertEquals(script, message.getScript());
        JobConfig jobConfig = message.getJobConfig();
        assertNotNull(jobConfig);
        assertEquals(nugetConfigXml, jobConfig.getNugetConfigXml());
    }

    @Test
    void createJobJobExists() throws Exception {
        // given
        // --- job is in the db
        Job savedJob = prepareAndSaveJob(JobStatus.PENDING,
                requestId, senderId, script, existingWorkerType, nugetConfigXml);

        assertNotNull(savedJob);
        assertTrue(jobsRepository.existsById(savedJob.getId()));
        // --- request contains the same requestId
        JobCreateRequest jobCreateRequest = prepareJobCreateRequest(requestId,
                "other-sender-id", "other-script", existingWorkerType, "<other-config />");

        MockHttpServletRequestBuilder jobCreationRequest = post("/jobs")
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
        assertEquals(requestId, jobRequest.getMessageId());
        assertEquals(senderId, jobRequest.getSenderId());
        com.naumov.dotnetscriptsscheduler.model.JobRequestPayload jobRequestModelPayload = jobRequest.getPayload();
        assertNotNull(jobRequestModelPayload);
        assertEquals(script, jobRequestModelPayload.getScript());
        assertEquals(existingWorkerType, jobRequestModelPayload.getAgentType());
        JobPayloadConfig jobPayloadConfig = jobRequestModelPayload.getJobPayloadConfig();
        assertNotNull(jobPayloadConfig);
        assertEquals(nugetConfigXml, jobPayloadConfig.getNugetConfigXml());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertNull(job.getResult());
        assertNotNull(job.getCreationOffsetDateTime());

        // --- message is not sent
        assertNull(consumedMessages.poll(10, TimeUnit.SECONDS));
    }

    @Test
    void createJobJobWrongAgentType() throws Exception {
        // given
        JobCreateRequest jobCreateRequest = prepareJobCreateRequest(requestId,
                senderId, script, notExistingWorkerType, nugetConfigXml);

        MockHttpServletRequestBuilder jobCreationRequest = post("/jobs")
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
                                  String nugetConfigXml) {
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

        return jobsRepository.saveAndFlush(job);
    }

    @Test
    void getJob() {
    }

    @Test
    void getJobRequest() {
    }

    @Test
    void getJobStatus() {
    }

    @Test
    void getJobResult() {
    }

    @Test
    void deleteJob() {
    }
}