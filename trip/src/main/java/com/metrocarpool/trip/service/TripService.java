package com.metrocarpool.trip.service;

import com.metrocarpool.contracts.proto.*;
import lombok.extern.slf4j.Slf4j;
import com.metrocarpool.trip.cache.TripCache;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    @Value("${kafka.topics.driver-ride-completion}")
    private String DRIVER_RIDE_COMPLETION_TOPIC;
    @Value("${kafka.topics.rider-ride-completion}")
    private String RIDER_RIDE_COMPLETION_TOPIC;
    @Value("${kafka.topics.driver-location-rider}")
    private String DRIVER_LOCATION_RIDER;

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TRIP_CACHE_KEY = "trip-cache";

    @KafkaListener(topics = "rider-driver-match", groupId = "trip-service")
    public void matchFound(byte[] message, Acknowledgment acknowledgment) {
        // Acknowledge that the message has been received
        try{
            log.info("Reached TripService.matchFound.");

            DriverRiderMatchEvent tempEvent = DriverRiderMatchEvent.parseFrom(message);
            Long driverId = tempEvent.getDriverId();
            Long riderId = tempEvent.getRiderId();
            String pickUpStation = tempEvent.getPickUpStation();
            acknowledgment.acknowledge();

            // Update the cache => push this pair {riderId, pickUpStation} in the list associated with key == driverId
            Map<Long, List<TripCache>> allTripCacheData = (Map<Long, List<TripCache>>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);
            assert allTripCacheData != null;
            allTripCacheData.get(driverId).add(TripCache.builder()
                    .riderId(riderId)
                    .pickUpStation(pickUpStation)
                    .build()
            );
            redisTemplate.opsForValue().set(TRIP_CACHE_KEY, allTripCacheData);
        } catch (InvalidProtocolBufferException e){
            log.error("Failed to parse DriverRiderMatchEvent message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "trip-completed", groupId = "trip-service")
    public void tripCompleted(byte[] message, Acknowledgment acknowledgment) {
        try {
            log.info("Reached TripService.tripCompleted.");

            DriverRideCompletionEvent tempEvent = DriverRideCompletionEvent.parseFrom(message);
            Long driverId = tempEvent.getDriverId();
            // Acknowledge that the message has been received
            acknowledgment.acknowledge();

            // Retrieve the trip cache from Redis
            Map<Long, List<TripCache>> allTripCacheData =
                    (Map<Long, List<TripCache>>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);

            if (allTripCacheData == null || !allTripCacheData.containsKey(driverId)) {
                // No record found for this driver; nothing to process
                return;
            }

            // Get the list of riders associated with this driver
            List<TripCache> riderList = allTripCacheData.get(driverId);
            if (riderList == null || riderList.isEmpty()) {
                // No riders associated, just remove the driver entry and return
                allTripCacheData.remove(driverId);
                redisTemplate.opsForValue().set(TRIP_CACHE_KEY, allTripCacheData);
                return;
            }

            // 1️⃣ Produce a Kafka event for the driver’s ride completion
            DriverRideCompletion driverRideCompletion = DriverRideCompletion.newBuilder()
                    .setDriverId(driverId)
                    .setEventMessage("Driver Ride Completed")
                    .build();
            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(DRIVER_RIDE_COMPLETION_TOPIC,
                    String.valueOf(driverId) ,driverRideCompletion.toByteArray());
            future.thenAccept(result -> {
                log.debug("Event = {} delivered to {}", driverRideCompletion, result.getRecordMetadata().topic());
            }).exceptionally(ex -> {
                log.error("Event failed. Error message = {}", ex.getMessage());
                // Optional: retry, put into Redis dead-letter queue
                return null;
            });

            // 2️⃣ Produce Kafka events for all associated riders
            for (TripCache riderTrip : riderList) {
                RiderRideCompletion riderRideCompletion = RiderRideCompletion.newBuilder()
                        .setRiderId(riderTrip.getRiderId())
                        .setEventMessage("Rider Ride Completed")
                        .build();
                CompletableFuture<SendResult<String, byte[]>> future1 = kafkaTemplate.send(RIDER_RIDE_COMPLETION_TOPIC,
                        String.valueOf(riderRideCompletion.getRiderId()), riderRideCompletion.toByteArray());
                future1.thenAccept(result -> {
                    log.debug("Event = {} delivered to {}", riderRideCompletion, result.getRecordMetadata().topic());
                }).exceptionally(ex -> {
                    log.error("Event failed. Error message = {}", ex.getMessage());
                    // Optional: retry, put into Redis dead-letter queue
                    return null;
                });
            }

            // 3️⃣ Remove the driver’s record from Redis after completion
            allTripCacheData.remove(driverId);
            redisTemplate.opsForValue().set(TRIP_CACHE_KEY, allTripCacheData);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse DriverRideCompletionEvent message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "driver-updates", groupId = "trip-service")
    public void driverLocationUpdates(byte[] message, Acknowledgment acknowledgment) {
        try {
            log.info("Reached TripService.driverLocationUpdates.");

            DriverLocationEvent driverLocationEvent = DriverLocationEvent.parseFrom(message);
            long driverId = driverLocationEvent.getDriverId();
            String oldStation = driverLocationEvent.getOldStation();
            String nextStation = driverLocationEvent.getNextStation();
            int timeToNextStation =  driverLocationEvent.getTimeToNextStation();
            acknowledgment.acknowledge();

            // Send driver location to all associated riders
            Map<Long, List<TripCache>> allTripCacheData =
                    (Map<Long, List<TripCache>>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);
            if (allTripCacheData == null || !allTripCacheData.containsKey(driverId)) {
                return;
            }

            List<TripCache> riderList = allTripCacheData.get(driverId);
            if (riderList == null || riderList.isEmpty()) {
                allTripCacheData.remove(driverId);
            }

            assert riderList != null;
            for (TripCache riderTrip : riderList) {
                DriverLocationForRiderEvent event = DriverLocationForRiderEvent.newBuilder()
                        .setDriverId(driverId)
                        .setRiderId(riderTrip.getRiderId())
                        .setTimeToNextStation(timeToNextStation)
                        .setOldStation(oldStation)
                        .setNextStation(nextStation)
                        .build();
                CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(DRIVER_LOCATION_RIDER,
                        String.valueOf(event.getDriverId()), event.toByteArray());
                future.thenAccept(result -> {
                    log.debug("Event = {} delivered to {}", event, result.getRecordMetadata().topic());
                }).exceptionally(ex -> {
                    log.error("Event failed. Error message = {}", ex.getMessage());
                    // Optional: retry, put into Redis dead-letter queue
                    return null;
                });
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse DriverLocationEvent message: {}", e.getMessage());
        }
    }
}