package com.metrocarpool.driver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DriverService {

    // ✅ Inject KafkaTemplate to publish events (assuming Spring Boot Kafka configured)
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String DRIVER_TOPIC = "driver-events";

    /**
     * Process the driver info and publish it as an event to Kafka
     *
     * @param driverId         Unique ID of the driver
     * @param routeStations    List of stations driver will pass
     * @param finalDestination Final destination of the driver
     * @return true if published successfully, false otherwise
     */
    public boolean processDriverInfo(Long driverId, List<String> routeStations, String finalDestination) {
        try {
            // ✅ Construct the event payload
            Map<String, Object> event = new HashMap<>();
            event.put("driverId", driverId);
            event.put("routeStations", routeStations);
            event.put("finalDestination", finalDestination);
            event.put("timestamp", System.currentTimeMillis());

            // ✅ Publish to Kafka topic
            kafkaTemplate.send(DRIVER_TOPIC, driverId.toString(), event);

            // Add this to Redis cache of driver service
            // code here

            log.info("🚗 Published driver event for ID {} to topic '{}': {}", driverId, DRIVER_TOPIC, event);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to process driver info for ID {}: {}", driverId, e.getMessage());
            return false;
        }
    }
}