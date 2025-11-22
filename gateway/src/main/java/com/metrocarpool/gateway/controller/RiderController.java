package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.RiderGrpcClient;
import com.metrocarpool.gateway.dto.PostRiderDTO;
import com.metrocarpool.gateway.dto.RiderStatusResponseDTO;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.google.protobuf.Timestamp;
import java.time.Instant;

@Builder
@RestController
@RequestMapping("/api/rider")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Slf4j
public class RiderController {
    @Autowired
    private RiderGrpcClient riderGrpcClient;

    @PostMapping(value = "/rider-info")
    public RiderStatusResponseDTO postRiderInformation(@RequestBody PostRiderDTO postRiderDTO) {
        log.info("RiderController.postRiderInformation.");

        // Call GRPC
        var response = riderGrpcClient.postRiderInfo(postRiderDTO);

        // Convert protobuf timestamp to ISO string
        String arrivalIso = convertProtoTimestampToIso(
                postRiderDTO.getArrivalTime()
        );

        return RiderStatusResponseDTO.builder()
                .STATUSSSSS(response.getStatus())
                .arrivalTime(arrivalIso)     // <-- send converted value
                .build();
    }


    private String convertProtoTimestampToIso(com.google.protobuf.Timestamp ts) {
        return java.time.Instant
                .ofEpochSecond(ts.getSeconds(), ts.getNanos())
                .toString();   // ISO 8601 string
    }
}
