package com.metrocarpool.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrocarpool.contracts.proto.*;
import com.metrocarpool.trip.redislock.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import com.metrocarpool.trip.cache.TripCache;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.TimeUnit;

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

    // Redis Distributed Lock
    private final RedisDistributedLock redisDistributedLock;
    private static final String redisTripLockKey = "lock:trip";

    // String template + mapper for tolerant reads of plain JSON (no @class)
    private final RedisTemplate<String, String> redisStringTemplate;
    private final ObjectMapper objectMapper;

    // Redis usage to ensure Kafka consumer idempotency
    private static final String RIDER_DRIVER_MATCH_KAFKA_DEDUP_KEY_PREFIX = "rider_driver_match_processed_kafka_msg:";
    private static final String TRIP_COMPLETED_KAFKA_DEDUP_KEY_PREFIX = "trip_completed_processed_kafka_msg:";
    private static final String DRIVER_UPDATES_KAFKA_DEDUP_KEY_PREFIX = "driver_updates_processed_kafka_msg:";
    private static final String DRIVER_LOCATION_RIDER_KAFKA_DEDUP_KEY_PREFIX = "driver_location_rider_processed_kafka_msg:";

    private boolean alreadyProcessed(String topicDedupKey, String messageId) {
        if (messageId == null) return false;

        String redisKey = topicDedupKey + messageId;

        return redisStringTemplate.hasKey(redisKey);
    }

    private void markProcessed(String topicDedupKey, String messageId) {
        if (messageId == null) return;

        String redisKey = topicDedupKey + messageId;

        // store marker with 24-hour TTL
        redisStringTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
    }

    private String tryAcquireLockWithRetry(String lockKey) {
        for (int attempt = 1; attempt <= 10; attempt++) {
            String lockValue = redisDistributedLock.acquireLock(lockKey, 5000);
            if (lockValue != null) {
                return lockValue;  // success
            }

            log.warn("Lock not acquired for key {}. Attempt {}/{}. Retrying in {} ms...",
                    lockKey, attempt, 10, (long) 200);

            try {
                Thread.sleep((long) 200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null; // all retries failed
    }

    @KafkaListener(topics = "rider-driver-match", groupId = "trip-service")
    public void matchFound(byte[] message, Acknowledgment acknowledgment) {
        // Try to acquire lock
        String lockValue = tryAcquireLockWithRetry(redisTripLockKey);
        if (lockValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisTripLockKey, 5000, 10, 200);
            return;
        }

        // Acknowledge that the message has been received
        try{
            log.info("Reached TripService.matchFound.");

            DriverRiderMatchEvent tempEvent = DriverRiderMatchEvent.parseFrom(message);
            String messageId = tempEvent.getMessageId();

            if (alreadyProcessed(RIDER_DRIVER_MATCH_KAFKA_DEDUP_KEY_PREFIX,  messageId)) {
                log.info("TripService.matchFound: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                acknowledgment.acknowledge();
                return;
            }

            Long driverId = tempEvent.getDriverId();
            Long riderId = tempEvent.getRiderId();
            String pickUpStation = tempEvent.getPickUpStation();

            // Acknowledge manually
            markProcessed(RIDER_DRIVER_MATCH_KAFKA_DEDUP_KEY_PREFIX, messageId);
            acknowledgment.acknowledge();

            // Update the cache => push this pair {riderId, pickUpStation} in the list associated with key == driverId
            Map<Long, List<TripCache>> allTripCacheData = (Map<Long, List<TripCache>>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);
            assert allTripCacheData != null;
            allTripCacheData.get(driverId).add(TripCache.builder()
                    .riderId(riderId)
                    .pickUpStation(pickUpStation)
                    .build()
            );

            log.info("Trip: Rider = {} is now travelling with Driver = {}", riderId, driverId);

            redisTemplate.opsForValue().set(TRIP_CACHE_KEY, allTripCacheData);
        } catch (InvalidProtocolBufferException e){
            log.error("Failed to parse DriverRiderMatchEvent message: {}", e.getMessage());
        } finally {
            redisDistributedLock.releaseLock(redisTripLockKey, lockValue);
        }
    }

    @KafkaListener(topics = "trip-completed", groupId = "trip-service")
    public void tripCompleted(byte[] message, Acknowledgment acknowledgment) {
        // Try to acquire lock
        String lockValue = tryAcquireLockWithRetry(redisTripLockKey);
        if (lockValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisTripLockKey, 5000, 10, 200);
            return;
        }

        try {
            log.info("Reached TripService.tripCompleted.");

            DriverRideCompletionEvent tempEvent = DriverRideCompletionEvent.parseFrom(message);
            String messageId = tempEvent.getMessageId();
            if (alreadyProcessed(TRIP_COMPLETED_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("NotificationService.tripCompleted: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                acknowledgment.acknowledge();
                return;
            }

            Long driverId = tempEvent.getDriverId();

            // Acknowledge that the message has been received
            markProcessed(TRIP_COMPLETED_KAFKA_DEDUP_KEY_PREFIX, messageId);
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
            DriverRideCompletionKafka driverRideCompletion = DriverRideCompletionKafka.newBuilder()
                    .setDriverId(driverId)
                    .setEventMessage("Driver Ride Completed")
                    .build();

            log.info("Trip completion: Driver = {}", driverId);

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
                RiderRideCompletionKafka riderRideCompletion = RiderRideCompletionKafka.newBuilder()
                        .setRiderId(riderTrip.getRiderId())
                        .setEventMessage("Rider Ride Completed")
                        .build();

                log.info("Trip completion: Rider = {}", riderTrip.getRiderId());

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
        } finally {
            redisDistributedLock.releaseLock(redisTripLockKey, lockValue);
        }
    }

    @KafkaListener(topics = "driver-updates", groupId = "trip-service")
    public void driverLocationUpdates(byte[] message, Acknowledgment acknowledgment) {
        // Try to acquire lock
        String lockValue = tryAcquireLockWithRetry(redisTripLockKey);
        if (lockValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisTripLockKey, 5000, 10, 200);
            return;
        }

        try {
            log.info("Reached TripService.driverLocationUpdates.");

            DriverLocationEvent driverLocationEvent = DriverLocationEvent.parseFrom(message);
            String messageId = driverLocationEvent.getMessageId();

            if (alreadyProcessed(DRIVER_UPDATES_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("TripService.driverLocationUpdates: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                acknowledgment.acknowledge();
                return;
            }

            long driverId = driverLocationEvent.getDriverId();
            String oldStation = driverLocationEvent.getOldStation();
            String nextStation = driverLocationEvent.getNextStation();
            int timeToNextStation =  driverLocationEvent.getTimeToNextStation();
            // Manually acknowledge
            markProcessed(DRIVER_UPDATES_KAFKA_DEDUP_KEY_PREFIX, messageId);
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

                log.info("Driver location: Location = {}", event);

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
        } finally {
            redisDistributedLock.releaseLock(redisTripLockKey, lockValue);
        }
    }
}