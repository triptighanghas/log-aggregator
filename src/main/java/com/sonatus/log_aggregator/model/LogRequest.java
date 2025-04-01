package com.sonatus.log_aggregator.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LogRequest {
    private String serviceName;
    private String timestamp;
    private String message;
}
