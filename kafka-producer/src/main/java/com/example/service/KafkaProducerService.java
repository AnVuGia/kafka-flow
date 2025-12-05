package com.example.service;

import com.example.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTemplate<String, Object> jsonKafkaTemplate;

    @Value("${app.kafka.topic.string}")
    private String stringTopic;

    @Value("${app.kafka.topic.json}")
    private String jsonTopic;

    /**
     * Send a simple string message to Kafka
     */
    public void sendMessage(String message) {
        log.info("Sending message to topic {}: {}", stringTopic, message);
        
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(stringTopic, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully to topic {} with offset {}", 
                    stringTopic, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic {}: {}", stringTopic, ex.getMessage());
            }
        });
    }

    /**
     * Send a message with a specific key (useful for partitioning)
     */
    public void sendMessageWithKey(String key, String message) {
        log.info("Sending message with key {} to topic {}: {}", key, stringTopic, message);
        
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(stringTopic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully to partition {} with offset {}", 
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message: {}", ex.getMessage());
            }
        });
    }

    /**
     * Send a JSON object message to Kafka
     */
    public Message sendJsonMessage(String content, String sender) {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender(sender)
                .timestamp(LocalDateTime.now())
                .build();

        log.info("Sending JSON message to topic {}: {}", jsonTopic, message);
        
        CompletableFuture<SendResult<String, Object>> future = 
            jsonKafkaTemplate.send(jsonTopic, message.getId(), message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("JSON message sent successfully to topic {} with offset {}", 
                    jsonTopic, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send JSON message: {}", ex.getMessage());
            }
        });

        return message;
    }

    /**
     * Send message to a specific topic
     */
    public void sendToTopic(String topic, String key, String message) {
        log.info("Sending message to custom topic {}: {}", topic, message);
        
        kafkaTemplate.send(topic, key, message)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Message sent to {} successfully", topic);
                } else {
                    log.error("Failed to send message to {}: {}", topic, ex.getMessage());
                }
            });
    }

    /**
     * Send message synchronously (blocks until acknowledgment)P
     */
    public SendResult<String, String> sendMessageSync(String message) throws Exception {
        log.info("Sending message synchronously: {}", message);
        return kafkaTemplate.send(stringTopic, message).get();
    }
}

