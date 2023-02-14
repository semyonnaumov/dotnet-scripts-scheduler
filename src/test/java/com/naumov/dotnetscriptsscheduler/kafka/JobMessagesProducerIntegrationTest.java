package com.naumov.dotnetscriptsscheduler.kafka;

import com.naumov.dotnetscriptsscheduler.AbstractIntegrationTest;
import com.naumov.dotnetscriptsscheduler.config.CommonConsumerPropertiesWrapper;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobConfig;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobTaskMessage;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobPayloadConfig;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.service.WorkerTypesService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Component
@SpringBootTest
@DirtiesContext
class JobMessagesProducerIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private CommonConsumerPropertiesWrapper props;
    private String workerType;
    @Value("${scheduler.kafka.jobs-topic-prefix}")
    private String jobsTopicPrefix;
    @Value("${scheduler.kafka-admin.jobs-topics-partitions}")
    private Integer jobsTopicsPartitions;
    @Autowired
    private WorkerTypesService workerTypesService;
    @Autowired
    private JobMessagesProducer jobMessagesProducer;
    private KafkaMessageListenerContainer<String, JobTaskMessage> jobTaskMessageListenerContainer;
    private BlockingQueue<ConsumerRecord<String, JobTaskMessage>> consumedMessages;

    @BeforeEach
    public void setUp() {
        consumedMessages = new LinkedBlockingQueue<>();
        workerType = workerTypesService.getAllWorkerTypes().stream().findFirst().orElseThrow(RuntimeException::new);

        String jobsTaskTopic = jobsTopicPrefix + workerType;
        ContainerProperties containerProperties = new ContainerProperties(jobsTaskTopic);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, JobTaskMessage.class.getName());

        DefaultKafkaConsumerFactory<String, JobTaskMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(props.toMap());

        jobTaskMessageListenerContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        jobTaskMessageListenerContainer.setupMessageListener((MessageListener<String, JobTaskMessage>) record -> consumedMessages.add(record));
        jobTaskMessageListenerContainer.start();

        ContainerTestUtils.waitForAssignment(jobTaskMessageListenerContainer, jobsTopicsPartitions);
    }

    @Test
    void sendJobTaskMessageAsync() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        JobRequestPayload jobRequestPayload = JobRequestPayload.builder()
                .script("some script")
                .agentType(workerType)
                .jobPayloadConfig(JobPayloadConfig.builder().nugetConfigXml("<config />").build())
                .build();

        Job job = Job.builder()
                .id(jobId)
                .request(JobRequest.builder().payload(jobRequestPayload).build())
                .build();

        jobMessagesProducer.sendJobTaskMessageAsync(job);

        ConsumerRecord<String, JobTaskMessage> received = consumedMessages.poll(10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(jobId.toString(), received.key());
        JobTaskMessage message = received.value();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
        assertEquals("some script", message.getScript());
        JobConfig jobConfig = message.getJobConfig();
        assertNotNull(jobConfig);
        assertEquals("<config />", jobConfig.getNugetConfigXml());
    }

    @AfterEach
    public void tearDown() {
        jobTaskMessageListenerContainer.stop();
    }
}