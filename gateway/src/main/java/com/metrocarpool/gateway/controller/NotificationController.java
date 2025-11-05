package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.NotificationGrpcClient;
import com.metrocarpool.notification.proto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationGrpcClient notificationGrpcClient;

    @GetMapping(value = "/matches", produces = "text/event-stream")
    public Flux<ServerSentEvent<RiderDriverMatch>> streamRiderDriverMatches(
            @RequestParam(defaultValue = "true") boolean status) {
        return notificationGrpcClient.getMatchNotifications(status)
                .map(match -> ServerSentEvent.builder(match).build())
                .doFinally(signal -> System.out.println("SSE connection closed: " + signal))
                .delayElements(Duration.ofMillis(100));
    }

    @GetMapping(value = "/driver-ride-completion", produces = "text/event-stream")
    public Flux<ServerSentEvent<DriverRideCompletion>> streamDriverRideCompletion(
            @RequestParam(defaultValue = "true") boolean status) {
        return notificationGrpcClient.getDriverCompletionNotifications(status)
                .map(event -> ServerSentEvent.builder(event).build())
                .doFinally(signal -> System.out.println("SSE connection closed: " + signal))
                .delayElements(Duration.ofMillis(100));
    }

    @GetMapping(value = "/rider-ride-completion", produces = "text/event-stream")
    public Flux<ServerSentEvent<RiderRideCompletion>> streamRiderRideCompletion(
            @RequestParam(defaultValue = "true") boolean status) {
        return notificationGrpcClient.getRiderCompletionNotifications(status)
                .map(event -> ServerSentEvent.builder(event).build())
                .doFinally(signal -> System.out.println("SSE connection closed: " + signal))
                .delayElements(Duration.ofMillis(100));
    }
}