package com.metrocarpool.gateway.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PostDriverDTO {
  private Long driverId;
  private List<String> routeStations;
  String finalDestination;
  Integer availableSeats;
}
