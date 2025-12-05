package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessageDto {
    
    private String topic;
    private int partition;
    private long offset;
    private String key;
    private String value;
    private String timestamp;
}

