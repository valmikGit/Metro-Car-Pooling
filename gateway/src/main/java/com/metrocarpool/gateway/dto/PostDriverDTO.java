package com.metrocarpool.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class PostDriverDTO {
    private Long driverId;
    private List<String> routeStations;
    String finalDestination;
}
