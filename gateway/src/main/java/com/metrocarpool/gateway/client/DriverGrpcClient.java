package com.metrocarpool.gateway.client;

import com.metrocarpool.driver.proto.DriverServiceGrpc;
import com.metrocarpool.driver.proto.DriverStatusResponse;
import com.metrocarpool.driver.proto.PostDriver;
import com.metrocarpool.gateway.dto.PostDriverDTO;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

@Component
public class DriverGrpcClient {
    private final DriverServiceGrpc.DriverServiceBlockingStub stub;

    public DriverGrpcClient() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("driver-service", 9090).usePlaintext().build();
        stub = DriverServiceGrpc.newBlockingStub(channel);
    }

    public DriverStatusResponse postDriverInfo(PostDriverDTO postDriverDTO) {
        PostDriver postDriver = PostDriver.newBuilder()
                .setDriverId(postDriverDTO.getDriverId())
                .addAllRouteStations(postDriverDTO.getRouteStations())
                .setFinalDestination(postDriverDTO.getFinalDestination())
                .setAvailableSeats(postDriverDTO.getAvailableSeats())
                .build();

        return stub.postDriverInfo(postDriver);
    }
}
