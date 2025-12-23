package com.metrocarpool.gateway.client;

import com.metrocarpool.notification.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class NotificationGrpcClient {

  @Autowired private DiscoveryClient discoveryClient;

  public NotificationGrpcClient() {
    log.info("Reached NotificationGrpcClient.NotificationGrpcClient.");
  }

  private ManagedChannel createChannel(ServiceInstance instance) {
    String host = instance.getHost();
    int port = getGrpcPort(instance);
    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  private NotificationServiceGrpc.NotificationServiceStub getStub(ManagedChannel channel) {
    return NotificationServiceGrpc.newStub(channel);
  }

  public Flux<RiderDriverMatch> getMatchNotifications(boolean status) {
    log.info("NotificationGrpcClient.getMatchNotifications.");

    ServiceInstance instance =
        discoveryClient.getInstances("notification").stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

    ManagedChannel channel = createChannel(instance);
    NotificationServiceGrpc.NotificationServiceStub stub = getStub(channel);

    NotificationInitiation request = NotificationInitiation.newBuilder().setStatus(status).build();

    return Flux.<RiderDriverMatch>create(
            sink -> {
              stub.matchNotificationInitiationPost(
                  request,
                  new StreamObserver<RiderDriverMatch>() {
                    @Override
                    public void onNext(RiderDriverMatch value) {
                      sink.next(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                      sink.error(t);
                    }

                    @Override
                    public void onCompleted() {
                      sink.complete();
                    }
                  });
            })
        .doFinally(
            signal -> {
              log.info("Stream terminated with signal: {}. Shutting down channel.", signal);
              channel.shutdown();
            });
  }

  public Flux<DriverRideCompletion> getDriverCompletionNotifications(boolean status) {
    log.info("NotificationGrpcClient.getDriverCompletionNotifications.");

    ServiceInstance instance =
        discoveryClient.getInstances("notification").stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

    ManagedChannel channel = createChannel(instance);
    NotificationServiceGrpc.NotificationServiceStub stub = getStub(channel);

    NotificationInitiation request = NotificationInitiation.newBuilder().setStatus(status).build();

    return Flux.<DriverRideCompletion>create(
            sink -> {
              stub.driverRideCompletionNotificationInitiationPost(
                  request,
                  new StreamObserver<DriverRideCompletion>() {
                    @Override
                    public void onNext(DriverRideCompletion value) {
                      sink.next(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                      sink.error(t);
                    }

                    @Override
                    public void onCompleted() {
                      sink.complete();
                    }
                  });
            })
        .doFinally(
            signal -> {
              log.info("Stream terminated with signal: {}. Shutting down channel.", signal);
              channel.shutdown();
            });
  }

  public Flux<RiderRideCompletion> getRiderCompletionNotifications(boolean status) {
    log.info("NotificationGrpcClient.getRiderCompletionNotifications.");

    ServiceInstance instance =
        discoveryClient.getInstances("notification").stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

    ManagedChannel channel = createChannel(instance);
    NotificationServiceGrpc.NotificationServiceStub stub = getStub(channel);

    NotificationInitiation request = NotificationInitiation.newBuilder().setStatus(status).build();

    return Flux.<RiderRideCompletion>create(
            sink -> {
              stub.riderRideCompletionNotificationInitiationPost(
                  request,
                  new StreamObserver<RiderRideCompletion>() {
                    @Override
                    public void onNext(RiderRideCompletion value) {
                      sink.next(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                      sink.error(t);
                    }

                    @Override
                    public void onCompleted() {
                      sink.complete();
                    }
                  });
            })
        .doFinally(
            signal -> {
              log.info("Stream terminated with signal: {}. Shutting down channel.", signal);
              channel.shutdown();
            });
  }

  public Flux<NotifyRiderDriverLocation> getDriverLocationForRiderNotifications(boolean status) {
    log.info("NotificationGrpcClient.getDriverLocationForRiderNotifications.");

    ServiceInstance instance =
        discoveryClient.getInstances("notification").stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Notification service not found in Eureka"));

    ManagedChannel channel = createChannel(instance);
    NotificationServiceGrpc.NotificationServiceStub stub = getStub(channel);

    NotificationInitiation request = NotificationInitiation.newBuilder().setStatus(status).build();

    return Flux.<NotifyRiderDriverLocation>create(
            sink -> {
              stub.driverLocationForRiderNotificationInitiationPost(
                  request,
                  new StreamObserver<NotifyRiderDriverLocation>() {
                    @Override
                    public void onNext(NotifyRiderDriverLocation value) {
                      sink.next(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                      sink.error(t);
                    }

                    @Override
                    public void onCompleted() {
                      sink.complete();
                    }
                  });
            })
        .doFinally(
            signal -> {
              log.info("Stream terminated with signal: {}. Shutting down channel.", signal);
              channel.shutdown();
            });
  }

  private int getGrpcPort(ServiceInstance instance) {
    log.info("NotificationGrpcClient.getGrpcPort.");
    String grpcPort = instance.getMetadata().get("grpc.port");
    return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
  }
}
