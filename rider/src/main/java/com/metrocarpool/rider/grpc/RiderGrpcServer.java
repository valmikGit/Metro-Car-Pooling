package com.metrocarpool.rider.grpc;

import com.metrocarpool.rider.proto.RiderServiceGrpc;
import com.metrocarpool.rider.service.RiderService;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Builder
public class RiderGrpcServer extends RiderServiceGrpc.RiderServiceImplBase {
    @Autowired
    private RiderService riderService;
}
