package com.metrocarpool.notification.service;

import com.metrocarpool.notification.proto.DriverRideCompletion;
import com.metrocarpool.notification.proto.RiderDriverMatch;
import com.metrocarpool.notification.proto.RiderRideCompletion;
import lombok.Builder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;

@Service
@Builder
public class NotificationService {
    private final Sinks.Many<RiderDriverMatch> riderDriverSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<DriverRideCompletion> driverCompletionSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<RiderRideCompletion> riderCompletionSink = Sinks.many().multicast().onBackpressureBuffer();

    // 🧠 This will be called by your Kafka listener whenever a new match event arrives.
    @KafkaListener(topics = "rider-driver-match", groupId = "notification-service")
    public void publishRiderDriverMatch(Long riderId, Long driverId, com.google.protobuf.Timestamp timestamp) {
        RiderDriverMatch match = RiderDriverMatch.newBuilder()
                .setRiderId(riderId)
                .setDriverId(driverId)
                .setDriverArrivalTime(timestamp)
                .build();

        riderDriverSink.tryEmitNext(match);
    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<RiderDriverMatch> streamRiderDriverMatches() {
        return riderDriverSink.asFlux();
    }

    // 🧠 This will be called by your Kafka listener whenever a new driver ride completion event arrives.
    @KafkaListener(topics = "driver-ride-completion", groupId = "notification-service")
    public void publishDriverRideCompletion(Long driverId, String message) {
        DriverRideCompletion completion = DriverRideCompletion.newBuilder()
                        .setDriverId(driverId)
                        .setCompletionMessage(message)
                        .build();

        driverCompletionSink.tryEmitNext(completion);
    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<DriverRideCompletion> streamDriverRideCompletions() {
        return driverCompletionSink.asFlux();
    }

    // 🧠 This will be called by your Kafka listener whenever a new rider ride completion event arrives.
    @KafkaListener(topics = "rider-ride-completion", groupId = "notification-service")
    public void publishRiderRideCompletion(Long riderId, String message) {
        RiderRideCompletion completion = RiderRideCompletion.newBuilder()
                .setRiderId(riderId)
                .setCompletionMessage(message)
                .build();

        riderCompletionSink.tryEmitNext(completion);
    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<RiderRideCompletion> streamRiderRideCompletions() {
        return riderCompletionSink.asFlux();
    }
}
