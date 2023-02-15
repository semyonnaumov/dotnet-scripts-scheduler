package com.naumov.dotnetscriptsscheduler.config;

import com.naumov.dotnetscriptsscheduler.config.props.SchedulerKafkaProperties;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobTaskMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfiguration {
    private final SchedulerKafkaProperties kafkaProperties;

    @Autowired
    public KafkaProducerConfiguration(SchedulerKafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public KafkaPropertyMapWrapper commonProducerProperties() {
        var props = new KafkaPropertyMapWrapper();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducerAcks());
        props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getReconnectBackoffMs());
        props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaProperties.getReconnectBackoffMaxMs());

        return props;
    }

    @Bean
    public ProducerFactory<String, JobTaskMessage> jobTaskMessageProducerFactory() {
        var producerFactory = new DefaultKafkaProducerFactory<String, JobTaskMessage>(commonProducerProperties().toMap());
        producerFactory.setProducerPerThread(true);

        return producerFactory;
    }

    @Bean
    public KafkaTemplate<String, JobTaskMessage> jobTaskMessageKafkaTemplate() {
        return new KafkaTemplate<>(jobTaskMessageProducerFactory());
    }
}
