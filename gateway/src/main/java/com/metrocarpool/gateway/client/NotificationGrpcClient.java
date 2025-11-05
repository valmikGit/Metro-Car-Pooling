package com.metrocarpool.gateway.client;

import com.metrocarpool.notification.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Iterator;

@Component
public class NotificationGrpcClient {
    private final NotificationServiceGrpc.NotificationServiceBlockingStub stub;

    public NotificationGrpcClient() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("notification-service", 9090)
                .usePlaintext()
                .build();
        stub = NotificationServiceGrpc.newBlockingStub(channel);
    }

    private <T> Flux<T> createReactiveStream(Iterator<T> iterator) {
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
                .doFinally(signal -> {
                    // Cleanup logic when stream is cancelled or completed
                    // You could log or close gRPC resources here if needed
                    System.out.println("Stream terminated with signal: " + signal);
                });
    }

    public Flux<RiderDriverMatch> getMatchNotifications(boolean status) {
        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return createReactiveStream(stub.matchNotificationInitiationPost(request))
                .share(); // allows multiple subscribers to share one stream
    }

    public Flux<DriverRideCompletion> getDriverCompletionNotifications(boolean status) {
        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return createReactiveStream(stub.driverRideCompletionNotificationInitiationPost(request))
                .share();
    }

    public Flux<RiderRideCompletion> getRiderCompletionNotifications(boolean status) {
        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return createReactiveStream(stub.riderRideCompletionNotificationInitiationPost(request))
                .share();
    }
}