package com.sonatus.log_aggregator.service;

import com.sonatus.log_aggregator.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.time.temporal.ChronoUnit;

@Service
public class LogStoreService {
    private static final Logger logger = LoggerFactory.getLogger(LogStoreService.class);

    //Internal map key->timestamp, internal map value is list of log entries for that timestamp
    //outer map key is service name, and value is internal map
    // Map: serviceName -> (timestamp -> list of log entries)
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, List<LogEntry>>> logsByService =
            new ConcurrentHashMap<>();

    public void addLog(LogEntry logEntry) {
        if (logEntry == null || logEntry.getServiceName() == null) {
            logger.warn("Invalid log entry: logEntry or serviceName is null.");
            return;
        }

        ConcurrentSkipListMap<Instant, List<LogEntry>> serviceLogs =
                logsByService.computeIfAbsent(logEntry.getServiceName(), k -> new ConcurrentSkipListMap<>());

        serviceLogs.compute(logEntry.getTimestamp(), (timestamp, list) -> {
            if (list == null) {
                list = Collections.synchronizedList(new ArrayList<>());
            }
            list.add(logEntry);
            return list;
        });
        logger.info("Added log entry for service {} at {}", logEntry.getServiceName(), logEntry.getTimestamp());

    }

    public List<LogEntry> queryLogs(String serviceName, Instant start, Instant end) {
        if (serviceName == null || start == null || end == null) {
            logger.error("Invalid query parameters. Service: {}, start: {}, end: {}", serviceName, start, end);
            return Collections.emptyList();
        }
        ConcurrentSkipListMap<Instant, List<LogEntry>> serviceLogs = logsByService.get(serviceName);
        if (serviceLogs == null) {
            logger.info("No logs found for service {}", serviceName);
            return Collections.emptyList();
        }

        SortedMap<Instant, List<LogEntry>> subMap = serviceLogs.subMap(start, true, end, true);
        List<LogEntry> result = new ArrayList<>();
        for (List<LogEntry> list : subMap.values()) {
            result.addAll(list);
        }
        logger.info("Query for service {} between {} and {} returned {} entries", serviceName, start, end, result.size());
        return result;
    }


    // Remove all log entries older than one hour for each service
    @Scheduled(fixedRate = 60000)       //scheduled to run every 1 minute, can be adjusted based on business need
    public void purgeOldLogs() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        logger.info("Purging logs older than {}", oneHourAgo);

        logsByService.forEach((service, serviceLogs) -> {
            SortedMap<Instant, List<LogEntry>> oldEntries = serviceLogs.headMap(oneHourAgo, false);
            List<Instant> keysToRemove = new ArrayList<>(oldEntries.keySet());
            for (Instant key : keysToRemove) {
                serviceLogs.remove(key);
                logger.debug("Purged logs for timestamp {} for service {}", key, service);
            }
        });
    }
}
