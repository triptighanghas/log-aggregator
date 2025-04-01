package com.sonatus.log_aggregator.model;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    private Instant timestamp;
    private String serviceName;
    private String message;

}
