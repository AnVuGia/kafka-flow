package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MathConsumerListener {

    private final MathProcessorService mathProcessorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Listen for math requests and automatically send replies using @SendTo
     * 
     * When using ReplyingKafkaTemplate on the producer side:
     * - The producer sets a REPLY_TOPIC header in the message
     * - @SendTo uses that header to know where to send the reply
     * - Correlation is handled automatically via CORRELATION_ID header
     * 
     * The return value of this method is automatically sent to the reply topic!
     */
    @KafkaListener(topics = "${app.kafka.topic.request}", groupId = "${spring.kafka.consumer.group-id}")
    @SendTo  // Replies to the topic specified in the REPLY_TOPIC header (set by ReplyingKafkaTemplate)
    public String listenMathRequests(String message) {
        log.info("Received math request: {}", message);

        try {
            // Parse the incoming message (expecting JSON with "expression" field)
            String expression = extractExpression(message);
            
            // Process the math expression
            String result = mathProcessorService.processExpression(expression);
            
            log.info("Calculated result for '{}': {}", expression, result);

            // Create response JSON - this will be automatically sent to the reply topic
            String response = String.format("{\"expression\":\"%s\",\"result\":\"%s\"}", 
                    expression, result);

            log.info("Sending reply: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
            
            // Return error response
            return String.format("{\"error\":\"%s\"}", e.getMessage());
        }
    }

    /**
     * Extract the math expression from the incoming message
     * Supports both plain text and JSON format
     */
    private String extractExpression(String message) {
        // Try to parse as JSON first
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("expression")) {
                return jsonNode.get("expression").asText();
            }
        } catch (Exception e) {
            // Not valid JSON, treat as plain text
        }
        
        // Return as plain text expression
        return message;
    }
}
