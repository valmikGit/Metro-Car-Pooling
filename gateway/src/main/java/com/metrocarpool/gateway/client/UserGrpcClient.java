package com.metrocarpool.gateway.client;

import com.metrocarpool.gateway.dto.DriverSignUpRequestDTO;
import com.metrocarpool.gateway.dto.RiderSignUpRequestDTO;
import com.metrocarpool.gateway.dto.UserLoginDTO;
import com.metrocarpool.user.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserGrpcClient {

    private final UserServiceGrpc.UserServiceBlockingStub stub;

    @Autowired
    private DiscoveryClient discoveryClient;

    public UserGrpcClient() {
        log.info("Reached UserGrpcClient.UserGrpcClient.");
        // Stub will be initialized lazily after service discovery
        this.stub = null;
    }

    private UserServiceGrpc.UserServiceBlockingStub getStub() {
        log.info("Reached UserGrpcClient.UserServiceBlockingStub.");

        // Discover the "user" service instance registered in Eureka
        ServiceInstance instance = discoveryClient.getInstances("user")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User service not found in Eureka"));

        String host = instance.getHost();
        int port = getGrpcPort(instance);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        return UserServiceGrpc.newBlockingStub(channel);
    }

    public SignUpOrLoginResponse DriverSignUpReq(DriverSignUpRequestDTO driverSignUpRequestDTO) {
        log.info("Reached UserGrpcClient.DriverSignUpReq.");

        UserServiceGrpc.UserServiceBlockingStub stub = getStub();

        DriverSignUp driverSignUp = DriverSignUp.newBuilder()
                .setUsername(driverSignUpRequestDTO.getUsername())
                .setPassword(driverSignUpRequestDTO.getPassword())
                .setLicenseId(driverSignUpRequestDTO.getLicenseId())
                .build();

        return stub.driverSignUpRequest(driverSignUp);
    }

    public SignUpOrLoginResponse RiderSignUpReq(RiderSignUpRequestDTO riderSignUpRequestDTO) {
        log.info("Reached UserGrpcClient.RiderSignUpReq.");

        UserServiceGrpc.UserServiceBlockingStub stub = getStub();

        RiderSignUp riderSignUp = RiderSignUp.newBuilder()
                .setUsername(riderSignUpRequestDTO.getUsername())
                .setPassword(riderSignUpRequestDTO.getPassword())
                .build();

        return stub.riderSignUpRequest(riderSignUp);
    }

    public SignUpOrLoginResponse DriverLoginReq(UserLoginDTO userLoginDTO) {
        log.info("Reached UserGrpcClient.DriverLoginReq.");

        UserServiceGrpc.UserServiceBlockingStub stub = getStub();

        DriverLogin driverLogin = DriverLogin.newBuilder()
                .setUsername(userLoginDTO.getUsername())
                .setPassword(userLoginDTO.getPassword())
                .build();

        return stub.driverLoginRequest(driverLogin);
    }

    public SignUpOrLoginResponse RiderLoginReq(UserLoginDTO userLoginDTO) {
        log.info("Reached UserGrpcClient.RiderLoginReq.");

        UserServiceGrpc.UserServiceBlockingStub stub = getStub();

        RiderLogin riderLogin = RiderLogin.newBuilder()
                .setUsername(userLoginDTO.getUsername())
                .setPassword(userLoginDTO.getPassword())
                .build();

        return stub.riderLoginRequest(riderLogin);
    }

    private int getGrpcPort(ServiceInstance instance) {
        log.info("Reached UserGrpcClient.getGrpcPort.");

        String grpcPort = instance.getMetadata().get("grpc.port");
        return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
    }
}