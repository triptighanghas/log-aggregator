package com.sonatus.log_aggregator.controller;

import com.sonatus.log_aggregator.model.LogEntry;
import com.sonatus.log_aggregator.model.LogRequest;
import com.sonatus.log_aggregator.model.LogResponse;
import com.sonatus.log_aggregator.service.LogStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/logs")
public class LogsController {
    private final LogStoreService logStoreService;
    private static final Logger logger = LoggerFactory.getLogger(LogsController.class);

    public LogsController(LogStoreService logStoreService) {
        this.logStoreService = logStoreService;
    }

    @PostMapping
    public ResponseEntity<String> createLog(@RequestBody LogRequest request) {
        // Validate input fields
        if (request.getServiceName() == null || request.getServiceName().isEmpty()) {
            logger.error("Invalid log request: Service name missing");
            return ResponseEntity.badRequest().body("Service name required.");
        }
        if (request.getTimestamp() == null || request.getTimestamp().isEmpty()) {
            logger.error("Invalid log request: Timestamp missing");
            return ResponseEntity.badRequest().body("Timestamp required.");
        }
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            logger.error("Invalid log request: Message missing");
            return ResponseEntity.badRequest().body("Message required.");
        }


        Instant timestamp;
        try {
            timestamp = Instant.parse(request.getTimestamp());
        } catch (Exception e) {
            logger.error("Invalid timestamp format: {}", request.getTimestamp());
            return ResponseEntity.badRequest().body("Timestamp format is invalid!");
        }
        LogEntry entry = new LogEntry(timestamp, request.getServiceName(), request.getMessage());
        logStoreService.addLog(entry);
        logger.info("Log entry created for service {} at {}", request.getServiceName(), timestamp);
        return ResponseEntity.ok("Log entry created successfully!");
    }

    @GetMapping
    public ResponseEntity<List<LogResponse>> getLogs(
            @RequestParam String service,
            @RequestParam String start,
            @RequestParam String end) {

        Instant startInstant;
        Instant endInstant;
        try {
            startInstant = Instant.parse(start);
            endInstant = Instant.parse(end);
        } catch (Exception e) {
            logger.error("Invalid date range provided. Start: {}, End: {}", start, end);
            return ResponseEntity.badRequest().build();
        }

        if (startInstant.isAfter(endInstant)) {
            logger.error("Invalid date range: start date {} is after end date {}", startInstant, endInstant);
            return ResponseEntity.badRequest().body(null);
        }


        List<LogEntry> results = logStoreService.queryLogs(service, startInstant, endInstant);
        List<LogResponse> responses = results.stream()
                .map(entry -> new LogResponse(entry.getTimestamp().toString(), entry.getMessage()))
                .collect(Collectors.toList());
        logger.info("Returning {} log entries for service {}", responses.size(), service);
        return ResponseEntity.ok(responses);
    }
}
