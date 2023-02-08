package com.naumov.dotnetscriptsscheduler.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.ManualAckListenerErrorHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerPayloadValidationErrorHandler implements ManualAckListenerErrorHandler {
    private static final Logger LOGGER = LogManager.getLogger(KafkaListenerPayloadValidationErrorHandler.class);

    @Override
    public Object handleError(Message<?> message,
                              ListenerExecutionFailedException exception,
                              Consumer<?, ?> consumer,
                              Acknowledgment ack) {
        if (exception.getCause() instanceof MethodArgumentNotValidException cause) {
            LOGGER.warn("Failed to validate kafka message {}: {}", message.getPayload(), cause.getMessage());
            if (ack != null) ack.acknowledge();

            return null;
        } else {
            throw exception;
        }
    }
}
