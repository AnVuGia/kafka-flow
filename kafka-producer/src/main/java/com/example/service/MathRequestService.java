package com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MathRequestService {

    private final ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic.math.request}")
    private String requestTopic;

    @Value("${app.kafka.topic.math.reply}")
    private String replyTopic;

    @Value("${app.kafka.reply.timeout:30}")
    private int replyTimeoutSeconds;

    /**
     * Send a math expression and wait for the result using ReplyingKafkaTemplate
     * This is the SYNCHRONOUS approach - blocks until reply is received
     */
    public String calculateAndWait(String expression) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        
        log.info("Sending math request with correlationId {}: {}", correlationId, expression);

        // Create the request JSON
        String requestJson = String.format("{\"expression\":\"%s\"}", expression);

        // Create ProducerRecord with the request topic
        ProducerRecord<String, String> record = new ProducerRecord<>(requestTopic, correlationId, requestJson);
        
        // ReplyingKafkaTemplate automatically:
        // 1. Generates correlation ID (or we can set our own)
        // 2. Sets the reply topic header
        // 3. Sends the message
        // 4. Waits for the reply with matching correlation ID
        RequestReplyFuture<String, String, String> future = replyingKafkaTemplate.sendAndReceive(record);

        log.info("Request sent, waiting for reply...");

        // Wait for the reply (blocking)
        ConsumerRecord<String, String> response = future.get(replyTimeoutSeconds, TimeUnit.SECONDS);
        
        String result = response.value();
        log.info("Received reply for correlationId {}: {}", correlationId, result);

        // Parse the result from JSON response
        return parseResult(result);
    }

    /**
     * Send a math expression without waiting (fire and forget)
     * This is the ASYNCHRONOUS approach - returns immediately with correlationId
     */
    public String sendCalculation(String expression) {
        String correlationId = UUID.randomUUID().toString();
        
        log.info("Sending math request (fire-and-forget) with correlationId {}: {}", correlationId, expression);

        String requestJson = String.format("{\"expression\":\"%s\"}", expression);

        // Use regular KafkaTemplate for fire-and-forget
        kafkaTemplate.send(requestTopic, correlationId, requestJson)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send math request: {}", ex.getMessage());
                    } else {
                        log.info("Math request sent successfully with correlationId {}", correlationId);
                    }
                });

        return correlationId;
    }

    /**
     * Parse the result from the JSON response
     */
    private String parseResult(String response) {
        try {
            // Simple JSON parsing - extract "result" field
            if (response.contains("\"result\"")) {
                int start = response.indexOf("\"result\":\"") + 10;
                int end = response.indexOf("\"", start);
                return response.substring(start, end);
            } else if (response.contains("\"error\"")) {
                int start = response.indexOf("\"error\":\"") + 9;
                int end = response.indexOf("\"", start);
                return "Error: " + response.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("Failed to parse response, returning raw: {}", response);
        }
        return response;
    }
}
