package com.metrocarpool.driver.service;

import com.metrocarpool.contracts.proto.DriverLocationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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

    private static final String DRIVER_TOPIC = "driver-updates";

    /**
     * Process the driver info and publish it as an event to Kafka
     *
     * @param driverId         Unique ID of the driver
     * @param routeStations    List of stations driver will pass
     * @param finalDestination Final destination of the driver
     * @return true if published successfully, false otherwise
     */
    public boolean processDriverInfo(Long driverId, List<String> routeStations, String finalDestination,
                                     Integer availableSeats) {
        try {
            // Update this driver in cache
            System.out.println("Driver ID: " + driverId);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to process driver info for ID {}: {}", driverId, e.getMessage());
            return false;
        }
    }

    @KafkaListener(topics = "rider-driver-match", groupId = "matching-service")
    public void matchFoundUpdateCache(Long driverId, Long riderId, String pickUpStation) {
        // Decrement the availableSeats by 1 for this driverId
        // If availableSeats == 0 => evict from cache
    }

    @Scheduled(fixedRate = 60000)
    public void cronJobDriverLocationSimulation() {
        // CRON job will simulate the location => Simulation logic
        DriverLocationEvent driverLocationEvent = DriverLocationEvent.newBuilder()
                .setDriverId(1)
                .setOldStation("blah blah")
                .setNextStation("blah")
                .setAvailableSeats(5)
                .setFinalDestination("A1")
                .build();

        kafkaTemplate.send(DRIVER_TOPIC, driverLocationEvent);
    }
}