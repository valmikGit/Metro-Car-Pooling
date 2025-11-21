package com.metrocarpool.user.grpc;

import com.metrocarpool.user.entity.DriverEntity;
import com.metrocarpool.user.entity.RiderEntity;
import com.metrocarpool.user.proto.*;
import com.metrocarpool.user.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Builder
@Slf4j
public class UserGrpcServer extends UserServiceGrpc.UserServiceImplBase {
    @Autowired
    private UserService userService;

    @Override
    public void driverSignUpRequest(DriverSignUp request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            log.info("Reached UserGrpcServer.driverSignUpRequest.");

            DriverEntity driverEntity = userService.driverSignUp(request.getUsername(), request.getPassword(),
                    request.getLicenseId());
            SignUpOrLoginResponse signUpOrLoginResponse = SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(driverEntity == null ? 401 : 200)
                    .setUserId(driverEntity == null ? -1 : driverEntity.getId())
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in UserGrpcServer.driverSignUpRequest = {}", e.getMessage());
            responseObserver.onError(e);
//            throw new RuntimeException(e);
        }
    }

    @Override
    public void riderSignUpRequest(RiderSignUp request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            log.info("Reached UserGrpcServer.riderSignUpRequest.");

            RiderEntity riderEntity = userService.riderSignUp(request.getUsername(), request.getPassword());
            SignUpOrLoginResponse signUpOrLoginResponse = SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(riderEntity == null ? 401 : 200)
                    .setUserId(riderEntity == null ? -1 : riderEntity.getId())
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in UserGrpcServer.riderSignUpRequest = {}", e.getMessage());
            responseObserver.onError(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void driverLoginRequest(DriverLogin request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            log.info("Reached UserGrpcServer.driverLoginRequest.");

            DriverEntity driverEntity = userService.driverLogin(request.getUsername(), request.getPassword());
            SignUpOrLoginResponse signUpOrLoginResponse =  SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(driverEntity == null ? 401 : 200)
                    .setUserId(driverEntity == null ? -1 : driverEntity.getId())
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in UserGrpcServer.driverLoginRequest = {}", e.getMessage());
            responseObserver.onError(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void riderLoginRequest(RiderLogin request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            log.info("Reached UserGrpcServer.riderLoginRequest.");

            RiderEntity riderEntity = userService.riderLogin(request.getUsername(), request.getPassword());
            SignUpOrLoginResponse signUpOrLoginResponse =   SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(riderEntity == null ? 401 : 200)
                    .setUserId(riderEntity == null ? -1 : riderEntity.getId())
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in UserGrpcServer.riderLoginRequest = {}", e.getMessage());
            responseObserver.onError(e);
            throw new RuntimeException(e);
        }
    }
}
