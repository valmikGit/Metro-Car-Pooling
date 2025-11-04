package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.NotificationGrpcClient;
import com.metrocarpool.notification.proto.DriverRideCompletion;
import com.metrocarpool.notification.proto.RiderDriverMatch;
import com.metrocarpool.notification.proto.RiderRideCompletion;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Iterator;

@RestController
@Builder
@RequestMapping("/api/notification")
public class NotificationController {
    @Autowired
    private NotificationGrpcClient notificationGrpcClient;

    @GetMapping(value = "/matches", produces = "text/event-stream")
    public Flux<ServerSentEvent<RiderDriverMatch>> streamRiderDriverMatches(@RequestParam(defaultValue = "true") boolean status) {
        return Flux.<ServerSentEvent<RiderDriverMatch>>create(emitter -> {
            try {
                Iterator<RiderDriverMatch> matches = notificationGrpcClient.getMatchNotifications(status);
                while (matches.hasNext()) {
                    RiderDriverMatch match = matches.next();
                    emitter.next(ServerSentEvent.builder(match).build());
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.error(e);
            }
        }).delayElements(Duration.ofMillis(100)); // optional pacing for frontend rendering
    }

    @GetMapping(value = "/driver-ride-completion", produces = "text/event-stream")
    public Flux<ServerSentEvent<DriverRideCompletion>> streamDriverRideCompletion(@RequestParam(defaultValue = "true") boolean status) {
        return Flux.<ServerSentEvent<DriverRideCompletion>>create(emitter -> {
            try {
                Iterator<DriverRideCompletion> matches = notificationGrpcClient.getDriverCompletionNotifications(status);
                while (matches.hasNext()) {
                    DriverRideCompletion match = matches.next();
                    emitter.next(ServerSentEvent.builder(match).build());
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.error(e);
            }
        }).delayElements(Duration.ofMillis(100)); // optional pacing for frontend rendering
    }

    @GetMapping(value = "/rider-ride-completion", produces = "text/event-stream")
    public Flux<ServerSentEvent<RiderRideCompletion>> streamRiderRideCompletion(@RequestParam(defaultValue = "true") boolean status) {
        return Flux.<ServerSentEvent<RiderRideCompletion>>create(emitter -> {
            try {
                Iterator<RiderRideCompletion> matches = notificationGrpcClient.getRiderCompletionNotifications(status);
                while (matches.hasNext()) {
                    RiderRideCompletion match = matches.next();
                    emitter.next(ServerSentEvent.builder(match).build());
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.error(e);
            }
        }).delayElements(Duration.ofMillis(100)); // optional pacing for frontend rendering
    }
}
