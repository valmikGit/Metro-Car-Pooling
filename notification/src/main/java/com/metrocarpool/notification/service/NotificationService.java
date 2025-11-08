package com.metrocarpool.notification.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.metrocarpool.contracts.proto.DriverRideCompletionEvent;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import com.metrocarpool.notification.proto.DriverRideCompletion;
import com.metrocarpool.notification.proto.RiderDriverMatch;
import com.metrocarpool.notification.proto.RiderRideCompletion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.kafka.support.Acknowledgment;

@Service
@Slf4j
public class NotificationService {
    private final Sinks.Many<RiderDriverMatch> riderDriverSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<DriverRideCompletion> driverCompletionSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<RiderRideCompletion> riderCompletionSink = Sinks.many().multicast().onBackpressureBuffer();
    
    // 🧠 This will be called by your Kafka listener whenever a new match event arrives.
    @KafkaListener(topics = "rider-driver-match", groupId = "notification-service")
    public void publishRiderDriverMatch(byte[] message, Acknowledgment ack) {
        try{
            DriverRiderMatchEvent tempEvent = DriverRiderMatchEvent.parseFrom(message);
            Long riderId = tempEvent.getRiderId();
            Long driverId = tempEvent.getDriverId();
            com.google.protobuf.Timestamp timestamp = tempEvent.getDriverArrivalTime();
            RiderDriverMatch match = RiderDriverMatch.newBuilder()
                    .setRiderId(riderId)
                    .setDriverId(driverId)
                    .setDriverArrivalTime(timestamp)
                    .build();

            riderDriverSink.tryEmitNext(match);

            //manually ACK
            ack.acknowledge();
        } catch (InvalidProtocolBufferException e){
            log.error("❌ Failed to parse DriverRiderMatchEvent message: {}", e);
        }

    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<RiderDriverMatch> streamRiderDriverMatches() {
        return riderDriverSink.asFlux();
    }

    // 🧠 This will be called by your Kafka listener whenever a new driver ride completion event arrives.
    @KafkaListener(topics = "driver-ride-completion", groupId = "notification-service")
    public void publishDriverRideCompletion(byte[] byteMessage, Acknowledgment ack) {
        try {
            DriverRideCompletion tempEvent = DriverRideCompletion.parseFrom(byteMessage);
            Long driverId = tempEvent.getDriverId();
            String message = tempEvent.getCompletionMessage();

            DriverRideCompletion completion = DriverRideCompletion.newBuilder()
                .setDriverId(driverId)
                .setCompletionMessage(message)
                .build();

            driverCompletionSink.tryEmitNext(completion);

            //manually ACK
            ack.acknowledge();
        } catch (InvalidProtocolBufferException e) {
            log.error("❌ Failed to parse DriverRideCompletionEvent message: {}", e);
        }
    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<DriverRideCompletion> streamDriverRideCompletions() {
        return driverCompletionSink.asFlux();
    }

    // 🧠 This will be called by your Kafka listener whenever a new rider ride completion event arrives.
    @KafkaListener(topics = "rider-ride-completion", groupId = "notification-service")
    public void publishRiderRideCompletion(byte[] byteMessage, Acknowledgment ack) {
        try{
            RiderRideCompletion tempEvent = RiderRideCompletion.parseFrom(byteMessage);
            Long riderId = tempEvent.getRiderId();
            String message = tempEvent.getCompletionMessage();
            RiderRideCompletion completion = RiderRideCompletion.newBuilder()
                    .setRiderId(riderId)
                    .setCompletionMessage(message)
                    .build();

            riderCompletionSink.tryEmitNext(completion);

            //manually ACK
            ack.acknowledge();
        } catch (InvalidProtocolBufferException e){
            log.error("❌ Failed to parse RiderRideCompletionEvent message: {}", e);
        }

    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<RiderRideCompletion> streamRiderRideCompletions() {
        return riderCompletionSink.asFlux();
    }
}
