package com.metrocarpool.trip.service;

import com.metrocarpool.contracts.proto.DriverRideCompletion;
import com.metrocarpool.contracts.proto.RiderRideCompletion;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String DRIVER_RIDE_COMPLETION_TOPIC = "driver-ride-completion";
    private static final String RIDER_RIDE_COMPLETION_TOPIC = "rider-ride-completion";

    @KafkaListener(topics = "match-found", groupId = "trip-service")
    public void matchFound(Long driverId, Long riderId, String pickUpStation) {
        // Update the cache => push this pair {riderId, pickUpStation} in the list associated with key == driverId
    }

    @KafkaListener(topics = "trip-completed", groupId = "trip-service")
    public void tripCompleted(Long driverId) {
        // Produce a sequence of Kafka events => ride completion for driver and all associated riders
        DriverRideCompletion driverRideCompletion = DriverRideCompletion.newBuilder()
                .setDriverId(driverId)
                .setEventMessage("Driver Ride Completed")
                .build();

        kafkaTemplate.send(DRIVER_RIDE_COMPLETION_TOPIC, driverRideCompletion);

        for (int i = 0; i < 5; i++) {
            RiderRideCompletion riderRideCompletion = RiderRideCompletion.newBuilder()
                    .setRiderId(1)
                    .setEventMessage("Rider Ride Completed")
                    .build();

            kafkaTemplate.send(RIDER_RIDE_COMPLETION_TOPIC, riderRideCompletion);
        }
    }
}
