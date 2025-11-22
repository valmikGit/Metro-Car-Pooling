package com.metrocarpool.gateway.client;

import com.metrocarpool.gateway.dto.PostRiderDTO;
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
        log.info("Reached RiderGrpcClient.RiderGrpcClient.");
    }

    private ManagedChannel createChannel(ServiceInstance instance) {
        String host = instance.getHost();
        int port = getGrpcPort(instance);
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private RiderServiceGrpc.RiderServiceBlockingStub getStub(ManagedChannel channel) {
        return RiderServiceGrpc.newBlockingStub(channel);
    }

    public RiderStatusResponse postRiderInfo(PostRiderDTO postRiderDTO) {
        log.info("Reached RiderGrpcClient.postRiderInfo.");

        ServiceInstance instance = discoveryClient.getInstances("rider")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rider service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            RiderServiceGrpc.RiderServiceBlockingStub stub = getStub(channel);

            PostRider postRider = PostRider.newBuilder()
                    .setRiderId(postRiderDTO.getRiderId())
                    .setPickUpStation(postRiderDTO.getPickUpStation())
                    .setArrivalTime(postRiderDTO.getArrivalTime())
                    .setDestinationPlace(postRiderDTO.getDestinationPlace())
                    .build();

            return stub.postRiderInfo(postRider);
        } catch (Exception e) {
            log.error("RiderGrpcClient.postRiderInfo: Error while posting rider info", e);
            return RiderStatusResponse.newBuilder()
                    .setStatus(false)
                    .build();
        } finally {
            channel.shutdown();
        }
    }

    private int getGrpcPort(ServiceInstance instance) {
        log.info("Reached RiderGrpcClient.getGrpcPort.");
        String grpcPort = instance.getMetadata().get("grpc.port");
        return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
    }
}