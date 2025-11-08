package com.metrocarpool.trip.service;

import lombok.extern.slf4j.Slf4j;
import com.metrocarpool.contracts.proto.DriverRideCompletion;
import com.metrocarpool.contracts.proto.DriverRideCompletionEvent;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import com.metrocarpool.contracts.proto.RiderRideCompletion;
import com.metrocarpool.trip.cache.TripCache;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private static final String DRIVER_RIDE_COMPLETION_TOPIC = "driver-ride-completion";
    private static final String RIDER_RIDE_COMPLETION_TOPIC = "rider-ride-completion";

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TRIP_CACHE_KEY = "trip-cache";

    @KafkaListener(topics = "rider-driver-match", groupId = "trip-service")
    public void matchFound(byte[] message, Acknowledgment acknowledgment) {
        // Acknowledge that the message has been received
        try{
            DriverRiderMatchEvent tempEvent = DriverRiderMatchEvent.parseFrom(message);
            Long driverId = tempEvent.getDriverId();
            Long riderId = tempEvent.getRiderId();
            String pickUpStation = tempEvent.getPickUpStation();
            acknowledgment.acknowledge();

            // Update the cache => push this pair {riderId, pickUpStation} in the list associated with key == driverId
            Map<Long, List<TripCache>> allTripCacheData = (Map<Long, List<TripCache>>) redisTemplate.opsForValue().get(TRIP_CACHE_KEY);
            allTripCacheData.get(driverId).add(TripCache.builder()
                    .riderId(riderId)
                    .pickUpStation(pickUpStation)
                    .build()
            );
            redisTemplate.opsForValue().set(TRIP_CACHE_KEY, allTripCacheData);
        } catch (InvalidProtocolBufferException e){
            log.error("❌ Failed to parse DriverRiderMatchEvent message: {}", e);
        }

    }

    @KafkaListener(topics = "trip-completed", groupId = "trip-service")
    public void tripCompleted(byte[] message, Acknowledgment acknowledgment) {
        try {
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
            kafkaTemplate.send(DRIVER_RIDE_COMPLETION_TOPIC, driverRideCompletion.toByteArray());

            // 2️⃣ Produce Kafka events for all associated riders
            for (TripCache riderTrip : riderList) {
                RiderRideCompletion riderRideCompletion = RiderRideCompletion.newBuilder()
                        .setRiderId(riderTrip.getRiderId())
                        .setEventMessage("Rider Ride Completed")
                        .build();
                kafkaTemplate.send(RIDER_RIDE_COMPLETION_TOPIC, riderRideCompletion.toByteArray());
            }

            // 3️⃣ Remove the driver’s record from Redis after completion
            allTripCacheData.remove(driverId);
            redisTemplate.opsForValue().set(TRIP_CACHE_KEY, allTripCacheData);
        } catch (InvalidProtocolBufferException e) {
            log.error("❌ Failed to parse DriverRideCompletionEvent message: {}", e);
        }
    }
}