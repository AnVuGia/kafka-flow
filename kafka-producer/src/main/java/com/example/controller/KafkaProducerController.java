package com.example.controller;

import com.example.dto.KafkaMessageDto;
import com.example.dto.MathRequest;
import com.example.dto.MathResponse;
import com.example.dto.MessageRequest;
import com.example.model.Message;
import com.example.service.KafkaConsumerService;
import com.example.service.KafkaProducerService;
import com.example.service.MathRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaProducerController {

    private final KafkaProducerService kafkaProducerService;
    private final KafkaConsumerService kafkaConsumerService;
    private final MathRequestService mathRequestService;

    // ==================== PRODUCER ENDPOINTS ====================

    /**
     * Send a simple string message
     * POST /api/kafka/send?message=Hello
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(@RequestParam String message) {
        log.info("Received request to send message: {}", message);
        kafkaProducerService.sendMessage(message);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "Message sent successfully");
        response.put("message", message);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Send a message with a key
     * POST /api/kafka/send-with-key?key=user123&message=Hello
     */
    @PostMapping("/send-with-key")
    public ResponseEntity<Map<String, String>> sendMessageWithKey(
            @RequestParam String key,
            @RequestParam String message) {
        log.info("Received request to send message with key {}: {}", key, message);
        kafkaProducerService.sendMessageWithKey(key, message);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "Message sent successfully");
        response.put("key", key);
        response.put("message", message);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Send a JSON message object
     * POST /api/kafka/send-json
     * Body: { "content": "Hello", "sender": "John" }
     */
    @PostMapping("/send-json")
    public ResponseEntity<Message> sendJsonMessage(@RequestBody MessageRequest request) {
        log.info("Received request to send JSON message: {}", request);
        Message sentMessage = kafkaProducerService.sendJsonMessage(request.getContent(), request.getSender());
        return ResponseEntity.ok(sentMessage);
    }

    /**
     * Send message to a specific topic
     * POST /api/kafka/send-to-topic?topic=my-topic&key=key1&message=Hello
     */
    @PostMapping("/send-to-topic")
    public ResponseEntity<Map<String, String>> sendToTopic(
            @RequestParam String topic,
            @RequestParam(required = false) String key,
            @RequestParam String message) {
        log.info("Received request to send message to topic {}: {}", topic, message);
        kafkaProducerService.sendToTopic(topic, key, message);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "Message sent successfully");
        response.put("topic", topic);
        response.put("message", message);
        if (key != null) {
            response.put("key", key);
        }
        
        return ResponseEntity.ok(response);
    }

    // ==================== CONSUMER ENDPOINTS ====================

    /**
     * List all available topics
     * GET /api/kafka/topics
     */
    @GetMapping("/topics")
    public ResponseEntity<Set<String>> listTopics() {
        log.info("Listing all topics");
        Set<String> topics = kafkaConsumerService.listTopics();
        return ResponseEntity.ok(topics);
    }

    /**
     * Get topic info (partitions, offsets, message count)
     * GET /api/kafka/topics/{topic}/info
     */
    @GetMapping("/topics/{topic}/info")
    public ResponseEntity<List<KafkaConsumerService.TopicPartitionInfo>> getTopicInfo(@PathVariable String topic) {
        log.info("Getting info for topic: {}", topic);
        List<KafkaConsumerService.TopicPartitionInfo> info = kafkaConsumerService.getTopicInfo(topic);
        return ResponseEntity.ok(info);
    }

    /**
     * Read messages from a topic
     * GET /api/kafka/messages/{topic}?max=100&fromBeginning=true
     */
    @GetMapping("/messages/{topic}")
    public ResponseEntity<List<KafkaMessageDto>> readMessages(
            @PathVariable String topic,
            @RequestParam(defaultValue = "100") int max,
            @RequestParam(defaultValue = "true") boolean fromBeginning) {
        log.info("Reading {} messages from topic {} (fromBeginning={})", max, topic, fromBeginning);
        List<KafkaMessageDto> messages = kafkaConsumerService.readMessages(topic, max, fromBeginning);
        return ResponseEntity.ok(messages);
    }

    // ==================== MATH CALCULATION ENDPOINTS ====================

    /**
     * Calculate a math expression and wait for the result
     * POST /api/kafka/calculate?expression=2+2
     * 
     * Flow: User -> Producer -> Kafka (math-requests) -> Consumer -> Kafka (math-replies) -> Producer -> User
     */
    @PostMapping("/calculate")
    public ResponseEntity<MathResponse> calculate(@RequestParam String expression) {
        log.info("Received calculation request: {}", expression);
        
        try {
            String result = mathRequestService.calculateAndWait(expression);
            
            MathResponse response = MathResponse.builder()
                    .expression(expression)
                    .result(result)
                    .status("SUCCESS")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Calculation failed: {}", e.getMessage());
            
            MathResponse errorResponse = MathResponse.builder()
                    .expression(expression)
                    .result(null)
                    .status("ERROR: " + e.getMessage())
                    .build();
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Calculate a math expression via JSON body
     * POST /api/kafka/calculate
     * Body: { "expression": "2+2" }
     */
    @PostMapping("/calculate-json")
    public ResponseEntity<MathResponse> calculateJson(@RequestBody MathRequest request) {
        log.info("Received JSON calculation request: {}", request.getExpression());
        
        try {
            String result = mathRequestService.calculateAndWait(request.getExpression());
            
            MathResponse response = MathResponse.builder()
                    .expression(request.getExpression())
                    .result(result)
                    .status("SUCCESS")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Calculation failed: {}", e.getMessage());
            
            MathResponse errorResponse = MathResponse.builder()
                    .expression(request.getExpression())
                    .result(null)
                    .status("ERROR: " + e.getMessage())
                    .build();
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Send a calculation request without waiting (fire and forget)
     * Returns correlationId for tracking
     * POST /api/kafka/calculate-async?expression=2+2
     */
    @PostMapping("/calculate-async")
    public ResponseEntity<Map<String, String>> calculateAsync(@RequestParam String expression) {
        log.info("Received async calculation request: {}", expression);
        
        String correlationId = mathRequestService.sendCalculation(expression);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "Request sent");
        response.put("expression", expression);
        response.put("correlationId", correlationId);
        response.put("message", "Check the math-replies topic for results");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Send a calculation request without waiting (fire and forget) via JSON body
     * Returns correlationId for tracking
     * POST /api/kafka/calculate-async-json
     * Body: { "expression": "2+2" }
     */
    @PostMapping("/calculate-async-json")
    public ResponseEntity<Map<String, String>> calculateAsyncJson(@RequestBody MathRequest request) {
        log.info("Received async JSON calculation request: {}", request.getExpression());
        
        String correlationId = mathRequestService.sendCalculation(request.getExpression());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "Request sent");
        response.put("expression", request.getExpression());
        response.put("correlationId", correlationId);
        response.put("message", "Check the math-replies topic for results");
        
        return ResponseEntity.ok(response);
    }

    // ==================== HEALTH CHECK ====================

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Kafka Producer");
        return ResponseEntity.ok(response);
    }
}
