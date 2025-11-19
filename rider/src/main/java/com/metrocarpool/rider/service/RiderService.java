package com.metrocarpool.rider.service;

import com.metrocarpool.contracts.proto.RiderRequestDriverEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiderService {
    // ✅ Inject KafkaTemplate to publish events (assuming Spring Boot Kafka configured)
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    @Value("${kafka.topics.rider-requests}")
    private String RIDER_TOPIC;

    /**
     * Process the driver info and publish it as an event to Kafka
     *
     * @param riderId         Unique ID of the rider
     * @param pickUpStation    pick up metro station of the rider
     * @param destinationPlace destination of the rider
     * @param arrivalTime arrival time of the rider at the pick up metro station
     * @return true if published successfully, false otherwise
     */
    public boolean processRiderInfo(Long riderId, String pickUpStation,
                                    String destinationPlace, com.google.protobuf.Timestamp arrivalTime) {
        try {
            log.info("Reached RiderService.processRiderInfo.");

            // ✅ Construct the event payload
            RiderRequestDriverEvent riderRequestDriverEvent = RiderRequestDriverEvent.newBuilder()
                    .setRiderId(riderId)
                    .setPickUpStation(pickUpStation)
                    .setArrivalTime(arrivalTime)
                    .setDestinationPlace(destinationPlace)
                    .build();

            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(RIDER_TOPIC, riderId.toString(),
                    riderRequestDriverEvent.toByteArray());
            future.thenAccept(result -> {
                log.debug("Event = {} delivered to {}", riderRequestDriverEvent, result.getRecordMetadata().topic());
            }).exceptionally(ex -> {
                log.error("Event failed. Error message = {}", ex.getMessage());
                // Optional: retry, put into Redis dead-letter queue
                return null;
            });

            log.info("Published rider event for ID {} to topic '{}': {}", riderId, RIDER_TOPIC, riderRequestDriverEvent);
            return true;
        } catch (Exception e) {
            log.error("Failed to process rider info for ID {}: {}", riderId, e.getMessage());
            return false;
        }
    }
}
