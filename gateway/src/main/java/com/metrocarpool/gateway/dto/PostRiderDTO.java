package com.metrocarpool.gateway.dto;

import lombok.Builder;

@Builder
public class PostRiderDTO {
    private Long riderId;
    private String pickUpStation;
    private String destinationPlace;
    private com.google.protobuf.Timestamp arrivalTime;
}
