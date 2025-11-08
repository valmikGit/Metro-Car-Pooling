package com.metrocarpool.user.grpc;

import com.metrocarpool.user.proto.*;
import com.metrocarpool.user.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Builder
public class UserGrpcServer extends UserServiceGrpc.UserServiceImplBase {
    @Autowired
    private UserService userService;

    @Override
    public void driverSignUpRequest(DriverSignUp request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            Boolean success = userService.driverSignUp(request.getUsername(), request.getPassword(),
                    request.getLicenseId());
            SignUpOrLoginResponse signUpOrLoginResponse = SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(success ? 200 : 401)
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            responseObserver.onError(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void riderSignUpRequest(RiderSignUp request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            Boolean success = userService.riderSignUp(request.getUsername(), request.getPassword());
            SignUpOrLoginResponse signUpOrLoginResponse = SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(success ? 200 : 401)
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            responseObserver.onError(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void driverLoginRequest(DriverLogin request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            Boolean success = userService.driverLogin(request.getUsername(), request.getPassword());
            SignUpOrLoginResponse signUpOrLoginResponse =  SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(success ? 200 : 401)
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            responseObserver.onError(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void riderLoginRequest(RiderLogin request, StreamObserver<SignUpOrLoginResponse> responseObserver) {
        try {
            Boolean success = userService.riderLogin(request.getUsername(), request.getPassword());
            SignUpOrLoginResponse signUpOrLoginResponse =   SignUpOrLoginResponse.newBuilder()
                    .setSTATUSCODE(success ? 200 : 401)
                    .build();
            responseObserver.onNext(signUpOrLoginResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            responseObserver.onError(e);
            throw new RuntimeException(e);
        }
    }
}
