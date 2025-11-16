package com.metrocarpool.driver.grpc;

import com.metrocarpool.driver.proto.DriverServiceGrpc;
import com.metrocarpool.driver.proto.DriverStatusResponse;
import com.metrocarpool.driver.proto.PostDriver;
import com.metrocarpool.driver.service.DriverService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class DriverGrpcServer extends DriverServiceGrpc.DriverServiceImplBase {

    private final DriverService driverService;

    @Override
    public void postDriverInfo(PostDriver request, StreamObserver<DriverStatusResponse> responseObserver) {
        try {
            // ✅ Call the business logic
            boolean success = driverService.processDriverInfo(
                    request.getDriverId(),
                    request.getRouteStationsList(),
                    request.getFinalDestination(),
                    request.getAvailableSeats()
            );

            // ✅ Build the response
            DriverStatusResponse response = DriverStatusResponse.newBuilder()
                    .setStatus(success)
                    .build();

            log.info("Reached DriverGrpcServer.postDriverInfo with request = {} and responseObserver = {}",
                    request, responseObserver);
            log.info("Driver Status Response: {}", response);

            // ✅ Send the response back to the client
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            // ❌ Handle any exception gracefully
            log.error("Error in DriverGrpcServer.postDriverInfo = ", e);
            responseObserver.onError(e);
        }
    }
}