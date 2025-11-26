package com.metrocarpool.rider.util;

import com.google.protobuf.Timestamp;
import com.metrocarpool.contracts.proto.RiderRequestDriverEvent;
import com.metrocarpool.rider.proto.PostRider;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility class for creating test data fixtures
 */
public class TestDataBuilder {

    /**
     * Build a PostRider gRPC request with default values
     */
    public static PostRider buildPostRiderRequest(Long riderId, String pickUpStation, 
                                                   String destinationPlace, Timestamp arrivalTime) {
        return PostRider.newBuilder()
                .setRiderId(riderId)
                .setPickUpStation(pickUpStation)
                .setDestinationPlace(destinationPlace)
                .setArrivalTime(arrivalTime)
                .build();
    }

    /**
     * Build a RiderRequestDriverEvent with default values
     */
    public static RiderRequestDriverEvent buildRiderRequestDriverEvent(Long riderId, 
                                                                        String pickUpStation,
                                                                        String destinationPlace,
                                                                        Timestamp arrivalTime) {
        return RiderRequestDriverEvent.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setRiderId(riderId)
                .setPickUpStation(pickUpStation)
                .setDestinationPlace(destinationPlace)
                .setArrivalTime(arrivalTime)
                .build();
    }

    /**
     * Build a protobuf Timestamp from current time
     */
    public static Timestamp buildTimestamp() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }

    /**
     * Build a protobuf Timestamp from specific instant
     */
    public static Timestamp buildTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
