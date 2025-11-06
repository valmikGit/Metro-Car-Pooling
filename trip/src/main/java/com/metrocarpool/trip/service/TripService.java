package com.metrocarpool.trip.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TripService {
    @KafkaListener(topics = "match-found", groupId = "trip-service")
    public void matchFound(Long driverId, Long riderId, String pickUpStation) {
        // Update the cache => push this pair {riderId, pickUpStation} in the list associated with key == driverId
    }

    @KafkaListener(topics = "trip-completed", groupId = "trip-service")
    public void tripCompleted(Long driverId) {
        // Produce a sequence of Kafka events => ride completion for driver and all associated riders
    }
}
