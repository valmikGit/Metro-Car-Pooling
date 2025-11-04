package com.metrocarpool.gateway.client;

import com.metrocarpool.rider.proto.RiderServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

@Component
public class RiderGrpcClient {
    private final RiderServiceGrpc.RiderServiceBlockingStub stub;

    public RiderGrpcClient() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("rider-service", 9090).usePlaintext().build();
        stub = RiderServiceGrpc.newBlockingStub(channel);
    }
}
