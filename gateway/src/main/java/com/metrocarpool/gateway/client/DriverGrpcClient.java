package com.metrocarpool.gateway.client;

import com.metrocarpool.driver.proto.DriverServiceGrpc;
import com.metrocarpool.driver.proto.DriverStatusResponse;
import com.metrocarpool.driver.proto.PostDriver;
import com.metrocarpool.gateway.dto.PostDriverDTO;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DriverGrpcClient {

    private final DriverServiceGrpc.DriverServiceBlockingStub stub;

    @Autowired
    private DiscoveryClient discoveryClient;

    public DriverGrpcClient() {
        // We’ll initialize stub later once DiscoveryClient is available
        this.stub = null;
    }

    private DriverServiceGrpc.DriverServiceBlockingStub getStub() {
        log.info("Reached DriverGrpcClient.getStub.");

        // Discover driver service from Eureka
        ServiceInstance instance = discoveryClient.getInstances("driver")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Driver service not found in Eureka"));

        String host = instance.getHost();
        int port = getGrpcPort(instance);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        return DriverServiceGrpc.newBlockingStub(channel);
    }

    public DriverStatusResponse postDriverInfo(PostDriverDTO postDriverDTO) {
        log.info("Reached DriverGrpcClient.postDriverInfo.");

        // Use discovered stub instead of static one
        DriverServiceGrpc.DriverServiceBlockingStub stub = getStub();

        PostDriver postDriver = PostDriver.newBuilder()
                .setDriverId(postDriverDTO.getDriverId())
                .addAllRouteStations(postDriverDTO.getRouteStations())
                .setFinalDestination(postDriverDTO.getFinalDestination())
                .setAvailableSeats(postDriverDTO.getAvailableSeats())
                .build();

        return stub.postDriverInfo(postDriver);
    }

    private int getGrpcPort(ServiceInstance instance) {
        log.info("Reached DriverGrpcClient.getGrpcPort.");

        String grpcPort = instance.getMetadata().get("grpc.port");
        return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
    }
}