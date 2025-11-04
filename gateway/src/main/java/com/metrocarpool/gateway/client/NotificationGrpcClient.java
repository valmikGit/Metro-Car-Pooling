package com.metrocarpool.gateway.client;

import com.metrocarpool.notification.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class NotificationGrpcClient {
    private final NotificationServiceGrpc.NotificationServiceBlockingStub stub;

    public NotificationGrpcClient() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("notification-service", 9090).usePlaintext().build();
        stub = NotificationServiceGrpc.newBlockingStub(channel);
    }

    public Iterator<RiderDriverMatch> getMatchNotifications(boolean status) {
        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return stub.matchNotificationInitiationPost(request);
    }

    public Iterator<DriverRideCompletion> getDriverCompletionNotifications(boolean status) {
        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return stub.driverRideCompletionNotificationInitiationPost(request);
    }

    public Iterator<RiderRideCompletion> getRiderCompletionNotifications(boolean status) {
        NotificationInitiation request = NotificationInitiation.newBuilder()
                .setStatus(status)
                .build();

        return stub.riderRideCompletionNotificationInitiationPost(request);
    }
}
