package com.metrocarpool.gateway.dto;

import lombok.Builder;

import java.util.List;

@Builder
public class PostDriverDTO {
    private Long driverId;
    private List<String> routeStations;
    String finalDestination;
}
