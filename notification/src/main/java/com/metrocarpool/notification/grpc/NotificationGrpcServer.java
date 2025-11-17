package com.metrocarpool.notification.grpc;

import com.metrocarpool.notification.proto.*;
import com.metrocarpool.notification.service.NotificationService;
import io.grpc.stub.StreamObserver;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Builder
@Slf4j
public class NotificationGrpcServer extends NotificationServiceGrpc.NotificationServiceImplBase {
    @Autowired
    private NotificationService notificationService;

    @Override
    public void matchNotificationInitiationPost(NotificationInitiation request, StreamObserver<RiderDriverMatch> responseObserver) {
        log.info("Reached NotificationGrpcServer.matchNotificationInitiationPost.");

        // Subscribe to the Flux and stream each event via gRPC
        notificationService.streamRiderDriverMatches()
                .doOnNext(responseObserver::onNext)
                .doOnComplete(responseObserver::onCompleted)
                .doOnError(responseObserver::onError)
                .subscribe();
    }

    @Override
    public void driverRideCompletionNotificationInitiationPost(NotificationInitiation request, StreamObserver<DriverRideCompletion> responseObserver) {
        log.info("Reached NotificationGrpcServer.driverRideCompletionNotificationInitiationPost.");

        notificationService.streamDriverRideCompletions()
                .doOnNext(responseObserver::onNext)
                .doOnComplete(responseObserver::onCompleted)
                .doOnError(responseObserver::onError)
                .subscribe();
    }

    @Override
    public void riderRideCompletionNotificationInitiationPost(NotificationInitiation request, StreamObserver<RiderRideCompletion> responseObserver) {
        log.info("Reached NotificationGrpcServer.riderRideCompletionNotificationInitiationPost.");

        notificationService.streamRiderRideCompletions()
                .doOnNext(responseObserver::onNext)
                .doOnComplete(responseObserver::onCompleted)
                .doOnError(responseObserver::onError)
                .subscribe();
    }

    @Override
    public void driverLocationForRiderNotificationInitiationPost(NotificationInitiation request, StreamObserver<NotifyRiderDriverLocation> responseObserver) {
        log.info("Reached NotificationGrpcServer.driverLocationForRiderNotificationInitiationPost.");

        notificationService.streamNotifyRiderDriverLocations()
                .doOnNext(responseObserver::onNext)
                .doOnComplete(responseObserver::onCompleted)
                .doOnError(responseObserver::onError)
                .subscribe();
    }
}
