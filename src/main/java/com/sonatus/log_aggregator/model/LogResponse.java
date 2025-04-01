package com.sonatus.log_aggregator.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LogResponse {
    private String timestamp;
    private String message;
}
