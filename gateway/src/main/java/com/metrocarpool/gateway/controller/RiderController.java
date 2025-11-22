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

    // Define a request DTO specifically for the JSON input
    @lombok.Data
    static class RiderRequest {
        private Long riderId;
        private String pickUpStation;
        private String destinationPlace;
        private String arrivalTime; // Accepts ISO String
    }

    @PostMapping(value = "/rider-info")
    public RiderStatusResponseDTO postRiderInformation(@RequestBody RiderRequest request) {
        log.info("RiderController.postRiderInformation.");
        log.info("Received request: {}", request);

        // We keep ISO in PostRiderDTO, because frontend sends ISO
        PostRiderDTO postRiderDTO = new PostRiderDTO();
        postRiderDTO.setRiderId(request.getRiderId());
        postRiderDTO.setPickUpStation(request.getPickUpStation());
        postRiderDTO.setDestinationPlace(request.getDestinationPlace());
        postRiderDTO.setArrivalTime(request.getArrivalTime()); // keep ISO string

        // Convert ISO → Protobuf Timestamp ONLY for GRPC
        Timestamp protoTimestamp = convertIsoToProtoTimestamp(request.getArrivalTime());

        // Send to gRPC client (update RiderGrpcClient to accept proto timestamp separately)
        var response = riderGrpcClient.postRiderInfo(
                postRiderDTO.getRiderId(),
                postRiderDTO.getPickUpStation(),
                postRiderDTO.getDestinationPlace(),
                protoTimestamp
        );

        return RiderStatusResponseDTO.builder()
                .STATUSSSSS(response.getStatus())
                .build();
    }

    private Timestamp convertIsoToProtoTimestamp(String isoString) {
        try {
            // Parse ISO 8601 string to Instant
            Instant instant = Instant.parse(isoString);
            
            // Convert to protobuf Timestamp
            return Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse ISO timestamp: {}", isoString, e);
            throw new IllegalArgumentException("Invalid ISO timestamp format: " + isoString);
        }
    }
}
