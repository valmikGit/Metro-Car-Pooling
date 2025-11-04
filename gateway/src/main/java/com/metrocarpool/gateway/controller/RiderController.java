package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.RiderGrpcClient;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Builder
@RestController
@RequestMapping("/api/rider")
public class RiderController {
    @Autowired
    private RiderGrpcClient riderGrpcClient;
}
