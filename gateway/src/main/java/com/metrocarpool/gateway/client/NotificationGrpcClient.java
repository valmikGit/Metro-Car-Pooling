package com.metrocarpool.gateway.client;

import com.metrocarpool.notification.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Iterator;

@Component
@Slf4j
public class NotificationGrpcClient {

    @Autowired
    private DiscoveryClient discoveryClient;

    public NotificationGrpcClient() {
        log.info("Reached NotificationGrpcClient.NotificationGrpcClient.");
    }

    private ManagedChannel createChannel(ServiceInstance instance) {
        String host = instance.getHost();
        int port = getGrpcPort(instance);
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private NotificationServiceGrpc.NotificationServiceBlockingStub getStub(ManagedChannel channel) {
        return NotificationServiceGrpc.newBlockingStub(channel);
    }

    private <T> Flux<T> createReactiveStream(Iterator<T> iterator) {
        log.info("NotificationGrpcClient.createReactiveStream.");
        return Flux.<T>create(sink -> {
                    try {
                        while (iterator.hasNext() && !sink.isCancelled()) {
                            sink.next(iterator.next());
                        }
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                })
                .doFinally(signal ->
                        System.out.println("Stream terminated with signal: " + signal)
                );
    }

    public Flux<RiderDriverMatch> getMatchNotifications(boolean status) {
        log.info("NotificationGrpcClient.getMatchNotifications.");

        ServiceInstance instance = discoveryClient.getInstances("notification")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub(channel);

            NotificationInitiation request = NotificationInitiation.newBuilder()
                    .setStatus(status)
                    .build();

            return createReactiveStream(stub.matchNotificationInitiationPost(request))
                    .share();
        } catch (Exception e) {
            log.error("NotificationGrpcClient.getMatchNotifications: NotificationGrpcClient.getMatchNotifications.", e);
            return Flux.empty();
        } finally {
            channel.shutdown();
        }
    }

    public Flux<DriverRideCompletion> getDriverCompletionNotifications(boolean status) {
        log.info("NotificationGrpcClient.getDriverCompletionNotifications.");

        ServiceInstance instance = discoveryClient.getInstances("notification")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub(channel);

            NotificationInitiation request = NotificationInitiation.newBuilder()
                    .setStatus(status)
                    .build();

            return createReactiveStream(stub.driverRideCompletionNotificationInitiationPost(request))
                    .share();
        } catch (Exception e) {
            log.error("NotificationGrpcClient.getDriverCompletionNotifications: NotificationGrpcClient.getDriverCompletionNotifications.", e);
            return Flux.empty();
        } finally {
            channel.shutdown();
        }
    }

    public Flux<RiderRideCompletion> getRiderCompletionNotifications(boolean status) {
        log.info("NotificationGrpcClient.getRiderCompletionNotifications.");

        ServiceInstance instance = discoveryClient.getInstances("notification")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub(channel);

            NotificationInitiation request = NotificationInitiation.newBuilder()
                    .setStatus(status)
                    .build();

            return createReactiveStream(stub.riderRideCompletionNotificationInitiationPost(request))
                    .share();
        } catch (Exception e) {
            log.error("NotificationGrpcClient.getRiderCompletionNotifications: NotificationGrpcClient.getRiderCompletionNotifications.", e);
            return Flux.empty();
        } finally {
            channel.shutdown();
        }
    }

    public Flux<NotifyRiderDriverLocation> getDriverLocationForRiderNotifications(boolean status) {
        log.info("NotificationGrpcClient.getDriverLocationForRiderNotifications.");

        ServiceInstance instance = discoveryClient.getInstances("notification")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub(channel);

            NotificationInitiation request = NotificationInitiation.newBuilder()
                    .setStatus(status)
                    .build();

            return createReactiveStream(stub.driverLocationForRiderNotificationInitiationPost(request))
                    .share();
        } catch (Exception e) {
            log.error("NotificationGrpcClient.getDriverLocationForRiderNotifications: NotificationGrpcClient.getDriverLocationForRiderNotifications.", e);
            return Flux.empty();
        } finally {
            channel.shutdown();
        }
    }

    private int getGrpcPort(ServiceInstance instance) {
        log.info("NotificationGrpcClient.getGrpcPort.");
        String grpcPort = instance.getMetadata().get("grpc.port");
        return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
    }
}