package com.metrocarpool.gateway.controller;

import com.google.protobuf.Timestamp;
import com.metrocarpool.gateway.client.RiderGrpcClient;
import com.metrocarpool.gateway.dto.RiderStatusResponseDTO;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rider")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Slf4j
public class RiderController {

  @Autowired private RiderGrpcClient riderGrpcClient;

  // Simple request DTO for JSON body
  @lombok.Data
  static class RiderRequest {
    private Long riderId;
    private String pickUpStation;
    private String destinationPlace;
    private String arrivalTime; // ISO 8601 string expected
  }

  @PostMapping(value = "/rider-info")
  public RiderStatusResponseDTO postRiderInformation(@RequestBody RiderRequest request) {
    log.info("RiderController.postRiderInformation.");
    log.info("Received request: {}", request);

    // Convert ISO -> protobuf Timestamp for gRPC call
    Timestamp protoTimestamp = convertIsoToProtoTimestamp(request.getArrivalTime());

    // Call gateway client (returns protobuf response mapped to POJO)
    var response =
        riderGrpcClient.postRiderInfo(
            request.getRiderId(),
            request.getPickUpStation(),
            request.getDestinationPlace(),
            protoTimestamp);

    // Build and return HTTP response DTO
    return RiderStatusResponseDTO.builder()
        .STATUSSSSS(response.getStatus()) // Make sure RiderStatusResponseDTO has 'status' field
        .build();
  }

  private Timestamp convertIsoToProtoTimestamp(String isoString) {
    try {
      Instant instant = Instant.parse(isoString);
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
