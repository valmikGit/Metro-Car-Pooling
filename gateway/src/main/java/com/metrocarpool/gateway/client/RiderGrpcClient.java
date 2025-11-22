package com.metrocarpool.gateway.client;

import com.google.protobuf.Timestamp;
import com.metrocarpool.rider.proto.PostRider;
import com.metrocarpool.rider.proto.RiderServiceGrpc;
import com.metrocarpool.rider.proto.RiderStatusResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RiderGrpcClient {

    @Autowired
    private DiscoveryClient discoveryClient;

    public RiderGrpcClient() {
        log.info("Initialized RiderGrpcClient");
    }

    private ManagedChannel createChannel(ServiceInstance instance) {
        String host = instance.getHost();
        int port = getGrpcPort(instance);

        log.info("Creating GRPC channel to rider-service at {}:{}", host, port);

        return ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private RiderServiceGrpc.RiderServiceBlockingStub getStub(ManagedChannel channel) {
        return RiderServiceGrpc.newBlockingStub(channel);
    }

    public RiderStatusResponse postRiderInfo(Long riderId, String pickUp, String dest, Timestamp arrivalTime) {
        log.info("Reached RiderGrpcClient.postRiderInfo.");

        ServiceInstance instance = discoveryClient.getInstances("rider")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rider service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);

        try {
            RiderServiceGrpc.RiderServiceBlockingStub stub = getStub(channel);

            // Build GRPC message
            PostRider postRider = PostRider.newBuilder()
                    .setRiderId(riderId)
                    .setPickUpStation(pickUp)
                    .setDestinationPlace(dest)
                    .setArrivalTime(arrivalTime)
                    .build();

            log.info("Sending PostRider GRPC request: {}", postRider);

            return stub.postRiderInfo(postRider);

        } catch (Exception e) {
            log.error("Error while posting rider info to GRPC", e);

            return RiderStatusResponse.newBuilder()
                    .setStatus(false)
                    .build();

        } finally {
            channel.shutdown();
        }
    }

    private int getGrpcPort(ServiceInstance instance) {
        log.info("Retrieving GRPC port from Eureka metadata");

        String grpcPort = instance.getMetadata().get("grpc.port");

        return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
    }
}
