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

    private final NotificationServiceGrpc.NotificationServiceBlockingStub stub;

    @Autowired
    private DiscoveryClient discoveryClient;

    public NotificationGrpcClient() {
        log.info("Reached NotificationGrpcClient.NotificationGrpcClient.");

        // Stub will be created lazily after Eureka discovery
        this.stub = null;
    }

    private NotificationServiceGrpc.NotificationServiceBlockingStub getStub() {
        log.info("Reached NotificationGrpcClient.NotificationServiceBlockingStub.");
        // Discover the "notification" service instance registered in Eureka
        ServiceInstance instance = discoveryClient.getInstances("notification")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

        String host = instance.getHost();
        int port = getGrpcPort(instance);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

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
        NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub();

        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return createReactiveStream(stub.matchNotificationInitiationPost(request))
                .share();
    }

    public Flux<DriverRideCompletion> getDriverCompletionNotifications(boolean status) {
        log.info("NotificationGrpcClient.getDriverCompletionNotifications.");
        NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub();

        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return createReactiveStream(stub.driverRideCompletionNotificationInitiationPost(request))
                .share();
    }

    public Flux<RiderRideCompletion> getRiderCompletionNotifications(boolean status) {
        log.info("NotificationGrpcClient.getRiderCompletionNotifications.");
        NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub();

        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return createReactiveStream(stub.riderRideCompletionNotificationInitiationPost(request))
                .share();
    }

    public Flux<NotifyRiderDriverLocation> getDriverLocationForRiderNotifications(boolean status) {
        log.info("NotificationGrpcClient.getDriverLocationForRiderNotifications.");
        NotificationServiceGrpc.NotificationServiceBlockingStub stub = getStub();

        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return createReactiveStream(stub.driverLocationForRiderNotificationInitiationPost(request))
                .share();
    }

    private int getGrpcPort(ServiceInstance instance) {
        log.info("NotificationGrpcClient.getGrpcPort.");
        String grpcPort = instance.getMetadata().get("grpc.port");
        return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
    }
}