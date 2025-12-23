package com.metrocarpool.gateway.client;

import com.google.protobuf.Timestamp;
import com.metrocarpool.rider.proto.PostRider;
import com.metrocarpool.rider.proto.RiderServiceGrpc;
import com.metrocarpool.rider.proto.RiderStatusResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RiderGrpcClient {

  @Autowired private DiscoveryClient discoveryClient;

  // Keep a reusable channel + stub
  private final AtomicReference<ManagedChannel> channelRef = new AtomicReference<>(null);
  private volatile RiderServiceGrpc.RiderServiceBlockingStub stub = null;
  private volatile String currentTarget = null; // "host:port" of current channel

  public RiderGrpcClient() {
    log.info("Initialized RiderGrpcClient");
  }

  private synchronized void ensureChannelFor(String host, int port) {
    String target = host + ":" + port;
    ManagedChannel ch = channelRef.get();

    // if channel exists and target same and not shutdown, reuse it
    if (ch != null && target.equals(currentTarget) && !ch.isShutdown() && !ch.isTerminated()) {
      return;
    }

    // otherwise create new channel and stub, shutting down previous
    if (ch != null) {
      try {
        log.info("Shutting down previous gRPC channel to {}", currentTarget);
        ch.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        log.warn("Interrupted while shutting down previous gRPC channel", e);
        Thread.currentThread().interrupt();
      } finally {
        channelRef.set(null);
      }
    }

    log.error("Creating GRPC channel to rider-service at {}:{}", host, port);
    ManagedChannel newChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

    channelRef.set(newChannel);
    this.stub = RiderServiceGrpc.newBlockingStub(newChannel);
    this.currentTarget = target;
  }

  private RiderServiceGrpc.RiderServiceBlockingStub getStubForInstance(ServiceInstance instance) {
    String host = instance.getHost();
    int port = getGrpcPort(instance);
    ensureChannelFor(host, port);
    return this.stub;
  }

  public RiderStatusResponse postRiderInfo(
      Long riderId, String pickUp, String dest, Timestamp arrivalTime) {
    log.info("Reached RiderGrpcClient.postRiderInfo.");

    ServiceInstance instance =
        discoveryClient.getInstances("rider").stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Rider service not found in Eureka"));

    // debug info from Eureka
    log.error("Eureka metadata = {}", instance.getMetadata());
    log.error("Eureka host = {}", instance.getHost());
    log.error("Eureka grpc.port = {}", instance.getMetadata().get("grpc.port"));

    try {
      RiderServiceGrpc.RiderServiceBlockingStub stub = getStubForInstance(instance);

      // Build GRPC message
      PostRider postRider =
          PostRider.newBuilder()
              .setRiderId(riderId)
              .setPickUpStation(pickUp)
              .setDestinationPlace(dest)
              .setArrivalTime(arrivalTime)
              .build();

      log.info("Sending PostRider GRPC request: {}", postRider);

      return stub.postRiderInfo(postRider);

    } catch (Exception e) {
      log.error("Error while posting rider info to GRPC", e);
      return RiderStatusResponse.newBuilder().setStatus(false).build();
    }
  }

  private int getGrpcPort(ServiceInstance instance) {
    log.info("Retrieving GRPC port from Eureka metadata");
    String grpcPort = instance.getMetadata().get("grpc.port");
    return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
  }

  @PreDestroy
  public void shutdown() {
    ManagedChannel ch = channelRef.get();
    if (ch != null && !ch.isShutdown()) {
      log.info("Shutting down gRPC channel to {}", currentTarget);
      ch.shutdown();
      try {
        ch.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        log.warn("Interrupted while waiting channel termination", e);
        Thread.currentThread().interrupt();
      }
    }
  }
}
