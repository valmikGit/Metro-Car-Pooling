package com.metrocarpool.gateway.controller;

import com.google.protobuf.util.JsonFormat;
import com.metrocarpool.gateway.client.NotificationGrpcClient;
import com.metrocarpool.notification.proto.*;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

@RestController
@RequestMapping("/api/notification")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

  private final NotificationGrpcClient notificationGrpcClient;

  @GetMapping(value = "/matches", produces = "text/event-stream")
  public Flux<ServerSentEvent<String>> streamRiderDriverMatches(
      @RequestParam(defaultValue = "true") boolean status) {
    log.info("Reached NotificationController.streamRiderDriverMatches.");
    return notificationGrpcClient
        .getMatchNotifications(status)
        .map(
            match -> {
              try {
                String json = JsonFormat.printer().print(match);
                return ServerSentEvent.builder(json).build();
              } catch (Exception e) {
                log.error("Error serializing match to JSON", e);
                return ServerSentEvent.builder("error").build();
              }
            })
        .doFinally(
            signal -> {
              if (signal.equals(SignalType.ON_ERROR)) {
                log.error("SSE connection closed: {}", signal);
              } else {
                log.info("SSE connection closed: {}", signal);
              }
            })
        .delayElements(Duration.ofMillis(100));
  }

  @GetMapping(value = "/driver-ride-completion", produces = "text/event-stream")
  public Flux<ServerSentEvent<String>> streamDriverRideCompletion(
      @RequestParam(defaultValue = "true") boolean status) {
    log.info("Reached NotificationController.streamDriverRideCompletion.");
    return notificationGrpcClient
        .getDriverCompletionNotifications(status)
        .map(
            event -> {
              try {
                String json = JsonFormat.printer().print(event);
                return ServerSentEvent.builder(json).build();
              } catch (Exception e) {
                log.error("Error serializing event to JSON", e);
                return ServerSentEvent.builder("error").build();
              }
            })
        .doFinally(
            signal -> {
              if (signal.equals(SignalType.ON_ERROR)) {
                log.error("SSE connection closed: {}", signal);
              } else {
                log.info("SSE connection closed: {}", signal);
              }
            })
        .delayElements(Duration.ofMillis(100));
  }

  @GetMapping(value = "/rider-ride-completion", produces = "text/event-stream")
  public Flux<ServerSentEvent<String>> streamRiderRideCompletion(
      @RequestParam(defaultValue = "true") boolean status) {
    log.info("Reached NotificationController.streamRiderRideCompletion.");
    return notificationGrpcClient
        .getRiderCompletionNotifications(status)
        .map(
            event -> {
              try {
                String json = JsonFormat.printer().print(event);
                return ServerSentEvent.builder(json).build();
              } catch (Exception e) {
                log.error("Error serializing event to JSON", e);
                return ServerSentEvent.builder("error").build();
              }
            })
        .doFinally(
            signal -> {
              if (signal.equals(SignalType.ON_ERROR)) {
                log.error("SSE connection closed: {}", signal);
              } else {
                log.info("SSE connection closed: {}", signal);
              }
            })
        .delayElements(Duration.ofMillis(100));
  }

  @GetMapping(value = "/driver-location-for-rider", produces = "text/event-stream")
  public Flux<ServerSentEvent<String>> streamDriverLocationForRider(
      @RequestParam(defaultValue = "true") boolean status) {
    log.info("Reached NotificationController.streamDriverLocationForRider.");
    return notificationGrpcClient
        .getDriverLocationForRiderNotifications(status)
        .map(
            event -> {
              try {
                String json = JsonFormat.printer().print(event);
                return ServerSentEvent.builder(json).build();
              } catch (Exception e) {
                log.error("Error serializing event to JSON", e);
                return ServerSentEvent.builder("error").build();
              }
            })
        .doFinally(
            signal -> {
              if (signal.equals(SignalType.ON_ERROR)) {
                log.error("SSE connection closed: {}", signal);
              } else {
                log.info("SSE connection closed: {}", signal);
              }
            })
        .delayElements(Duration.ofMillis(100));
  }
}
