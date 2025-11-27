package com.metrocarpool.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.metrocarpool.contracts.proto.*;
import com.metrocarpool.notification.proto.DriverRideCompletion;
import com.metrocarpool.notification.proto.NotifyRiderDriverLocation;
import com.metrocarpool.notification.proto.RiderDriverMatch;
import com.metrocarpool.notification.proto.RiderRideCompletion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.kafka.support.Acknowledgment;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final Sinks.Many<RiderDriverMatch> riderDriverSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<DriverRideCompletion> driverCompletionSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<RiderRideCompletion> riderCompletionSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<NotifyRiderDriverLocation>  driverLocationForRiderSink = Sinks.many().multicast().onBackpressureBuffer();

    // String template + mapper for tolerant reads of plain JSON (no @class)
    private final RedisTemplate<String, String> redisStringTemplate;
    private final ObjectMapper objectMapper;

    // Redis usage to ensure Kafka consumer idempotency
    private static final String RIDER_DRIVER_MATCH_KAFKA_DEDUP_KEY_PREFIX = "rider_driver_match_processed_kafka_msg:";
    private static final String DRIVER_RIDE_COMPLETION_KAFKA_DEDUP_KEY_PREFIX = "driver_rider_completion_processed_kafka_msg:";
    private static final String RIDER_RIDE_COMPLETION_KAFKA_DEDUP_KEY_PREFIX = "rider_rider_completion_processed_kafka_msg:";
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
    
    // 🧠 This will be called by your Kafka listener whenever a new match event arrives.
    @KafkaListener(topics = "${kafka.topics.rider-driver-match}", groupId = "${spring.kafka.consumer.group-id}")
    public void publishRiderDriverMatch(byte[] message, Acknowledgment ack) {
        try{
            log.info("Reached NotificationService.publishRiderDriverMatch.");

            DriverRiderMatchEvent tempEvent = DriverRiderMatchEvent.parseFrom(message);
            String messageId = tempEvent.getMessageId();
            if (alreadyProcessed(RIDER_DRIVER_MATCH_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("NotificationService.publishRiderDriverMatch: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                ack.acknowledge();
                return;
            }

            long riderId = tempEvent.getRiderId();
            long driverId = tempEvent.getDriverId();
            com.google.protobuf.Timestamp timestamp = tempEvent.getDriverArrivalTime();
            RiderDriverMatch match = RiderDriverMatch.newBuilder()
                    .setRiderId(riderId)
                    .setDriverId(driverId)
                    .setDriverArrivalTime(timestamp)
                    .build();

            riderDriverSink.tryEmitNext(match);

            //manually ACK
            markProcessed(RIDER_DRIVER_MATCH_KAFKA_DEDUP_KEY_PREFIX, messageId);
            ack.acknowledge();
        } catch (InvalidProtocolBufferException e){
            log.error("Failed to parse DriverRiderMatchEvent message: {}", e.getMessage());
        }
    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<RiderDriverMatch> streamRiderDriverMatches() {
        log.info("Reached NotificationService.streamRiderDriverMatches.");
        return riderDriverSink.asFlux();
    }

    // 🧠 This will be called by your Kafka listener whenever a new driver ride completion event arrives.
    @KafkaListener(topics = "${kafka.topics.driver-ride-completion}", groupId = "${spring.kafka.consumer.group-id}")
    public void publishDriverRideCompletion(byte[] byteMessage, Acknowledgment ack) {
        try {
            log.info("Reached NotificationService.publishDriverRideCompletion.");

            DriverRideCompletionKafka tempEvent = DriverRideCompletionKafka.parseFrom(byteMessage);
            String messageId = tempEvent.getMessageId();
            if (alreadyProcessed(DRIVER_RIDE_COMPLETION_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("NotificationService.publishDriverRideCompletion: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                return;
            }

            long driverId = tempEvent.getDriverId();
            String message = tempEvent.getEventMessage();

            DriverRideCompletion completion = DriverRideCompletion.newBuilder()
                .setDriverId(driverId)
                .setCompletionMessage(message)
                .build();

            driverCompletionSink.tryEmitNext(completion);

            //manually ACK
            markProcessed(DRIVER_RIDE_COMPLETION_KAFKA_DEDUP_KEY_PREFIX, messageId);
            ack.acknowledge();
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse DriverRideCompletionEvent message: {}", e.getMessage());
        }
    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<DriverRideCompletion> streamDriverRideCompletions() {
        log.info("Reached NotificationService.streamDriverRideCompletions.");
        return driverCompletionSink.asFlux();
    }

    // 🧠 This will be called by your Kafka listener whenever a new rider ride completion event arrives.
    @KafkaListener(topics = "${kafka.topics.rider-ride-completion}", groupId = "${spring.kafka.consumer.group-id}")
    public void publishRiderRideCompletion(byte[] byteMessage, Acknowledgment ack) {
        try{
            log.info("Reached NotificationService.publishRiderRideCompletion.");

            RiderRideCompletionKafka tempEvent = RiderRideCompletionKafka.parseFrom(byteMessage);
            String messageId = tempEvent.getMessageId();
            if (alreadyProcessed(RIDER_RIDE_COMPLETION_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("NotificationService.publishRiderRideCompletion: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                ack.acknowledge();
                return;
            }

            long riderId = tempEvent.getRiderId();
            String message = tempEvent.getEventMessage();
            RiderRideCompletion completion = RiderRideCompletion.newBuilder()
                    .setRiderId(riderId)
                    .setCompletionMessage(message)
                    .build();

            riderCompletionSink.tryEmitNext(completion);

            //manually ACK
            markProcessed(RIDER_RIDE_COMPLETION_KAFKA_DEDUP_KEY_PREFIX, messageId);
            ack.acknowledge();
        } catch (InvalidProtocolBufferException e){
            log.error("Failed to parse RiderRideCompletionEvent message: {}", e.getMessage());
        }

    }

    // 🔁 This will be called by the gRPC server to stream to clients.
    public Flux<RiderRideCompletion> streamRiderRideCompletions() {
        log.info("Reached NotificationService.streamRiderRideCompletions.");
        return riderCompletionSink.asFlux();
    }

    @KafkaListener(topics = "${kafka.topics.driver-location-rider}", groupId = "${spring.kafka.consumer.group-id}")
    public void publishDriverLocationForRiderEvent(byte[] byteMessage, Acknowledgment ack) {
        try {
            log.info("Reached NotificationService.publishDriverLocationForRiderEvent.");

            DriverLocationForRiderEvent driverLocationForRiderEvent = DriverLocationForRiderEvent.parseFrom(byteMessage);
            String messageId = driverLocationForRiderEvent.getMessageId();
            if (alreadyProcessed(DRIVER_LOCATION_RIDER_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("NotificationService.driverLocationForRiderEvent: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                ack.acknowledge();
                return;
            }
            // manually acknowledge the message
            markProcessed(DRIVER_LOCATION_RIDER_KAFKA_DEDUP_KEY_PREFIX, messageId);
            ack.acknowledge();

            NotifyRiderDriverLocation notifyRiderDriverLocation = NotifyRiderDriverLocation.newBuilder()
                    .setDriverId(driverLocationForRiderEvent.getDriverId())
                    .setRiderId(driverLocationForRiderEvent.getRiderId())
                    .setNextStation(driverLocationForRiderEvent.getNextStation())
                    .setTimeToNextStation(driverLocationForRiderEvent.getTimeToNextStation())
                    .build();
            driverLocationForRiderSink.tryEmitNext(notifyRiderDriverLocation);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse NotifyRiderDriverLocationEvent message: {}", e.getMessage());
        }
    }

    public Flux<NotifyRiderDriverLocation> streamNotifyRiderDriverLocations() {
        log.info("Reached NotificationService.streamNotifyRiderDriverLocations.");
        return driverLocationForRiderSink.asFlux();
    }
}
