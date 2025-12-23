package com.metrocarpool.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRiderDTO {
  private Long riderId;
  private String pickUpStation;
  private String destinationPlace;
  // private com.google.protobuf.Timestamp arrivalTime;
  private String arrivalTime; // ISO String now
}
