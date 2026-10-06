package dev.forgeflow.worker.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOffWithMaxRetries;

/**
 * Mirrors forgeflow-api's error handling: exponential backoff then dead-letter, so a job that
 * fails to even start (e.g. a malformed PipelineQueuedEvent) does not retry forever against the
 * same partition.
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
