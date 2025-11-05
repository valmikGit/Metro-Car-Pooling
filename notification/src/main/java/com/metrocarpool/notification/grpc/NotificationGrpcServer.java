package com.metrocarpool.notification.grpc;

import com.metrocarpool.notification.proto.*;
import com.metrocarpool.notification.service.NotificationService;
import io.grpc.stub.StreamObserver;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Builder
public class NotificationGrpcServer extends NotificationServiceGrpc.NotificationServiceImplBase {
    @Autowired
    private NotificationService notificationService;

    @Override
    public void matchNotificationInitiationPost(NotificationInitiation request, StreamObserver<RiderDriverMatch> responseObserver) {
        // Subscribe to the Flux and stream each event via gRPC
        notificationService.streamRiderDriverMatches()
                .doOnNext(responseObserver::onNext)
                .doOnComplete(responseObserver::onCompleted)
                .doOnError(responseObserver::onError)
                .subscribe();
    }

    @Override
    public void driverRideCompletionNotificationInitiationPost(NotificationInitiation request, StreamObserver<DriverRideCompletion> responseObserver) {
        notificationService.streamDriverRideCompletions()
                .doOnNext(responseObserver::onNext)
                .doOnComplete(responseObserver::onCompleted)
                .doOnError(responseObserver::onError)
                .subscribe();
    }

    @Override
    public void riderRideCompletionNotificationInitiationPost(NotificationInitiation request, StreamObserver<RiderRideCompletion> responseObserver) {
        notificationService.streamRiderRideCompletions()
                .doOnNext(responseObserver::onNext)
                .doOnComplete(responseObserver::onCompleted)
                .doOnError(responseObserver::onError)
                .subscribe();
    }
}
