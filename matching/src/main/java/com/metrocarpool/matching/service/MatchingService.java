package com.metrocarpool.matching.service;

import com.google.protobuf.Timestamp;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String MATCHING_TOPIC = "rider-driver-match";

    @KafkaListener(topics = "driver-updates", groupId = "matching-service")
    public void driverInfoUpdateCache(Long driverId, String oldStation, String nextStation,
                                      Duration timeToNextStation, Integer availableSeats,
                                      String finalDestination) {
        // Update in Redis cache
    }

    @KafkaListener(topics = "rider-requests", groupId = "matching-service")
    public void riderInfoDriverMatchingAlgorithm(Long riderId, String pickUpStation,
                                                 com.google.protobuf.Timestamp arrivalTime,
                                                 String destinationPlace,
                                                 Acknowledgment acknowledgment) {
        acknowledgment.acknowledge();
        // Write the matching algorithm
        // Write the Kafka producer in case of a match
        DriverRiderMatchEvent driverRiderMatchEvent = DriverRiderMatchEvent.newBuilder()
                .setDriverId(1)
                .setRiderId(riderId)
                .setPickUpStation(pickUpStation)
                .setDriverArrivalTime(arrivalTime)
                .build();

        kafkaTemplate.send(MATCHING_TOPIC, driverRiderMatchEvent);
        // Push the rider in the waiting queue if a driver is currently not available
    }

    @Scheduled(cron = "* * * * * *")
    public void cronJobMatchingAlgorithm() {
        // Run this CRON job every second to check whether there is a driver for the riders in the waiting queue
        // Write the matching algorithm
        // Write the Kafka producer logic here after matching
        DriverRiderMatchEvent driverRiderMatchEvent = DriverRiderMatchEvent.newBuilder()
                .setDriverId(1)
                .setRiderId(0)
                .setPickUpStation("")
                .setDriverArrivalTime(Timestamp.newBuilder()
                        .setSeconds(1)
                        .setNanos(0)
                        .build())
                .build();

        kafkaTemplate.send(MATCHING_TOPIC, driverRiderMatchEvent);
    }
}
