package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.DriverGrpcClient;
import com.metrocarpool.gateway.dto.DriverStatusResponseDTO;
import com.metrocarpool.gateway.dto.PostDriverDTO;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Builder
@RestController
@RequestMapping("/api/driver")
public class DriverController {
    @Autowired
    private DriverGrpcClient driverGrpcClient;

    @PostMapping(value = "/driver-info")
    public DriverStatusResponseDTO postDriverInformation(@RequestBody PostDriverDTO postDriverDTO) {
        return DriverStatusResponseDTO.builder()
                .status(driverGrpcClient.postDriverInfo(postDriverDTO).getStatus())
                .build();
    }
}
