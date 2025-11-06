package com.metrocarpool.matching.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MatchingService {
    @KafkaListener(topics = "driver-updates", groupId = "matching-service")
    public void driverInfoUpdateCache(Long driverId, String oldStation, String nextStation, Duration timeToNextStation,
                                  Integer availableSeats, String finalDestination) {
        // Update in Redis cache
    }

    @KafkaListener(topics = "rider-requests", groupId = "matching-service")
    public void riderInfoDriverMatchingAlgorithm(Long riderId, String pickUpStation,
                                                 com.google.protobuf.Timestamp arrivalTime, String destinationPlace) {
        // Write the Kafka producer in case of a match
        // Push the rider in the waiting queue if a driver is currently not available
    }

    @Scheduled(cron = "* * * * * *")
    public void cronJobMatchingAlgorithm() {
        // Run this CRON job every second to check whether there is a driver for the riders in the waiting queue
        // Write the Kafka producer logic here after matching
    }
}
