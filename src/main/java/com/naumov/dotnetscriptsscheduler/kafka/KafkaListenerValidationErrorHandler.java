package com.naumov.dotnetscriptsscheduler.kafka;

import jakarta.validation.ValidationException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.ManualAckListenerErrorHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerValidationErrorHandler implements ManualAckListenerErrorHandler {
    private static final Logger LOGGER = LogManager.getLogger(KafkaListenerValidationErrorHandler.class);

    @Override
    public Object handleError(Message<?> message,
                              ListenerExecutionFailedException exception,
                              Consumer<?, ?> consumer,
                              Acknowledgment ack) {
        if (exception.getCause() instanceof ValidationException) {
            LOGGER.warn("Kafka message validation failure: {}", message.getPayload(), exception);
            if (ack != null) ack.acknowledge();

            return null;
        } else {
            throw exception;
        }
    }
}
