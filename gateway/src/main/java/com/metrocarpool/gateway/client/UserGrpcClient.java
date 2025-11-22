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

    @Autowired
    private DiscoveryClient discoveryClient;

    public UserGrpcClient() {
        log.info("Reached UserGrpcClient.UserGrpcClient.");
    }

    private ManagedChannel createChannel(ServiceInstance instance) {
        String host = instance.getHost();
        int port = getGrpcPort(instance);
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private UserServiceGrpc.UserServiceBlockingStub getStub(ManagedChannel channel) {
        return UserServiceGrpc.newBlockingStub(channel);
    }

    public SignUpOrLoginResponse DriverSignUpReq(DriverSignUpRequestDTO driverSignUpRequestDTO) {
        log.info("Reached UserGrpcClient.DriverSignUpReq.");

        ServiceInstance instance = discoveryClient.getInstances("user")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            UserServiceGrpc.UserServiceBlockingStub stub = getStub(channel);

            DriverSignUp driverSignUp = DriverSignUp.newBuilder()
                    .setUsername(driverSignUpRequestDTO.getUsername())
                    .setPassword(driverSignUpRequestDTO.getPassword())
                    .setLicenseId(driverSignUpRequestDTO.getLicenseId())
                    .build();

            return stub.driverSignUpRequest(driverSignUp);
        } catch (Exception e) {
            log.error("UserGrpcClient.DriverSignUpReq: DriverSignUpReq failed.", e);
            return SignUpOrLoginResponse.newBuilder()
                    .setUserId(-1)
                    .setSTATUSCODE(401)
                    .build();
        } finally {
            channel.shutdown();
        }
    }

    public SignUpOrLoginResponse RiderSignUpReq(RiderSignUpRequestDTO riderSignUpRequestDTO) {
        log.info("Reached UserGrpcClient.RiderSignUpReq.");

        ServiceInstance instance = discoveryClient.getInstances("user")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            UserServiceGrpc.UserServiceBlockingStub stub = getStub(channel);

            RiderSignUp riderSignUp = RiderSignUp.newBuilder()
                    .setUsername(riderSignUpRequestDTO.getUsername())
                    .setPassword(riderSignUpRequestDTO.getPassword())
                    .build();

            return stub.riderSignUpRequest(riderSignUp);
        } catch (Exception e) {
            log.error("UserGrpcClient.RiderSignUpReq: RiderSignUpReq failed.", e);
            return SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(401)
                    .setUserId(-1)
                    .build();
        } finally {
            channel.shutdown();
        }
    }

    public SignUpOrLoginResponse DriverLoginReq(UserLoginDTO userLoginDTO) {
        log.info("Reached UserGrpcClient.DriverLoginReq.");

        ServiceInstance instance = discoveryClient.getInstances("user")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            UserServiceGrpc.UserServiceBlockingStub stub = getStub(channel);

            DriverLogin driverLogin = DriverLogin.newBuilder()
                    .setUsername(userLoginDTO.getUsername())
                    .setPassword(userLoginDTO.getPassword())
                    .build();

            return stub.driverLoginRequest(driverLogin);
        } catch (Exception e) {
            log.error("UserGrpcClient.DriverLoginReq: DriverLoginReq failed.", e);
            return SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(401)
                    .setUserId(-1)
                    .build();
        } finally {
            channel.shutdown();
        }
    }

    public SignUpOrLoginResponse RiderLoginReq(UserLoginDTO userLoginDTO) {
        log.info("Reached UserGrpcClient.RiderLoginReq.");

        ServiceInstance instance = discoveryClient.getInstances("user")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User service not found in Eureka"));

        ManagedChannel channel = createChannel(instance);
        try {
            UserServiceGrpc.UserServiceBlockingStub stub = getStub(channel);

            RiderLogin riderLogin = RiderLogin.newBuilder()
                    .setUsername(userLoginDTO.getUsername())
                    .setPassword(userLoginDTO.getPassword())
                    .build();

            return stub.riderLoginRequest(riderLogin);
        }  catch (Exception e) {
            log.error("UserGrpcClient.RiderLoginReq: RiderLoginReq failed.", e);
            return SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(401)
                    .setUserId(-1)
                    .build();
        } finally {
            channel.shutdown();
        }
    }

    private int getGrpcPort(ServiceInstance instance) {
        log.info("Reached UserGrpcClient.getGrpcPort.");
        String grpcPort = instance.getMetadata().get("grpc.port");
        return grpcPort != null ? Integer.parseInt(grpcPort) : 9090;
    }
}