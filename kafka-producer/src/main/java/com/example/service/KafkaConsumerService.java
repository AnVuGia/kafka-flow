package com.example.service;

import com.example.dto.KafkaMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class KafkaConsumerService {

    private final ConsumerFactory<String, String> consumerFactory;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public KafkaConsumerService(ConsumerFactory<String, String> consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    /**
     * Read messages from a topic (from beginning or last N messages)
     */
    public List<KafkaMessageDto> readMessages(String topic, int maxMessages, boolean fromBeginning) {
        List<KafkaMessageDto> messages = new ArrayList<>();
        
        try (KafkaConsumer<String, String> consumer = 
                (KafkaConsumer<String, String>) consumerFactory.createConsumer("reader-" + System.currentTimeMillis(), null)) {
            
            // Get partitions for the topic
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            
            consumer.assign(partitions);
            
            if (fromBeginning) {
                consumer.seekToBeginning(partitions);
            } else {
                // Seek to end and go back maxMessages
                consumer.seekToEnd(partitions);
                for (TopicPartition partition : partitions) {
                    long endOffset = consumer.position(partition);
                    long startOffset = Math.max(0, endOffset - maxMessages);
                    consumer.seek(partition, startOffset);
                }
            }
            
            // Poll for messages
            int pollAttempts = 0;
            while (messages.size() < maxMessages && pollAttempts < 3) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    if (messages.size() >= maxMessages) break;
                    
                    messages.add(KafkaMessageDto.builder()
                            .topic(record.topic())
                            .partition(record.partition())
                            .offset(record.offset())
                            .key(record.key())
                            .value(record.value())
                            .timestamp(formatTimestamp(record.timestamp()))
                            .build());
                }
                
                if (records.isEmpty()) {
                    pollAttempts++;
                }
            }
            
            log.info("Read {} messages from topic {}", messages.size(), topic);
            
        } catch (Exception e) {
            log.error("Error reading messages from topic {}: {}", topic, e.getMessage());
            throw new RuntimeException("Failed to read messages from topic: " + topic, e);
        }
        
        return messages;
    }

    /**
     * Get list of all topics
     */
    public Set<String> listTopics() {
        try (KafkaConsumer<String, String> consumer = 
                (KafkaConsumer<String, String>) consumerFactory.createConsumer("topic-lister", null)) {
            Set<String> topics = consumer.listTopics().keySet();
            // Filter out internal topics
            topics.removeIf(t -> t.startsWith("__"));
            return topics;
        } catch (Exception e) {
            log.error("Error listing topics: {}", e.getMessage());
            throw new RuntimeException("Failed to list topics", e);
        }
    }

    /**
     * Get topic info (partitions, offsets)
     */
    public List<TopicPartitionInfo> getTopicInfo(String topic) {
        List<TopicPartitionInfo> info = new ArrayList<>();
        
        try (KafkaConsumer<String, String> consumer = 
                (KafkaConsumer<String, String>) consumerFactory.createConsumer("info-reader", null)) {
            
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(p -> new TopicPartition(topic, p.partition()))
                    .toList();
            
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);
            
            for (TopicPartition partition : partitions) {
                long endOffset = consumer.position(partition);
                consumer.seekToBeginning(Collections.singletonList(partition));
                long beginOffset = consumer.position(partition);
                
                info.add(new TopicPartitionInfo(
                        partition.partition(),
                        beginOffset,
                        endOffset,
                        endOffset - beginOffset
                ));
            }
            
        } catch (Exception e) {
            log.error("Error getting topic info for {}: {}", topic, e.getMessage());
            throw new RuntimeException("Failed to get topic info: " + topic, e);
        }
        
        return info;
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public record TopicPartitionInfo(int partition, long beginOffset, long endOffset, long messageCount) {}
}

