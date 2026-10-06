package dev.forgeflow.api.common.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOffWithMaxRetries;

/**
 * Shared error handling for all @KafkaListener consumers: exponential backoff (1s base,
 * doubling, capped at 5 retries) and then publish-to-dead-letter-topic via
 * DeadLetterPublishingRecoverer, so a poison-pill message cannot block a partition forever.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", record.partition()));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}
