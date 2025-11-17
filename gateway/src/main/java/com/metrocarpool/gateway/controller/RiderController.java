package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.RiderGrpcClient;
import com.metrocarpool.gateway.dto.PostRiderDTO;
import com.metrocarpool.gateway.dto.RiderStatusResponseDTO;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Builder
@RestController
@RequestMapping("/api/rider")
@Slf4j
public class RiderController {
    @Autowired
    private RiderGrpcClient riderGrpcClient;

    @PostMapping(value = "/rider-info")
    public RiderStatusResponseDTO postRiderInformation(@RequestBody PostRiderDTO postRiderDTO) {
        log.info("RiderController.postRiderInformation.");
        return RiderStatusResponseDTO.builder()
                .status(riderGrpcClient.postRiderInfo(postRiderDTO).getStatus())
                .build();
    }
}
