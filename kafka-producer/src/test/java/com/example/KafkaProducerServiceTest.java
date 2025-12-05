package com.example;

import com.example.model.Message;
import com.example.service.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class KafkaProducerServiceTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    void contextLoads() {
        assertNotNull(kafkaProducerService);
    }

    @Test
    void testSendMessage() {
        assertDoesNotThrow(() -> kafkaProducerService.sendMessage("Test message"));
    }

    @Test
    void testSendMessageWithKey() {
        assertDoesNotThrow(() -> kafkaProducerService.sendMessageWithKey("key1", "Test message with key"));
    }

    @Test
    void testSendJsonMessage() {
        Message message = kafkaProducerService.sendJsonMessage("Test content", "TestSender");
        
        assertNotNull(message);
        assertNotNull(message.getId());
        assertEquals("Test content", message.getContent());
        assertEquals("TestSender", message.getSender());
        assertNotNull(message.getTimestamp());
    }
}

