package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.DriverGrpcClient;
import com.metrocarpool.gateway.dto.DriverStatusResponseDTO;
import com.metrocarpool.gateway.dto.PostDriverDTO;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Builder
@RestController
@RequestMapping("/api/driver")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Slf4j
public class DriverController {
    @Autowired
    private DriverGrpcClient driverGrpcClient;

    @PostMapping(value = "/driver-info")
    public DriverStatusResponseDTO postDriverInformation(@RequestBody PostDriverDTO postDriverDTO) {
        log.info("Reached DriverController.postDriverInformation.");
        return DriverStatusResponseDTO.builder()
                .STATUSSSSS(driverGrpcClient.postDriverInfo(postDriverDTO).getStatus())
                .build();
    }
}
