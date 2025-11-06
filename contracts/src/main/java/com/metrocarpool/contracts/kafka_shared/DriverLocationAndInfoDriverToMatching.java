package com.metrocarpool.contracts.kafka_shared;

import lombok.*;

import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationAndInfoDriverToMatching {
    private Long driverId;
    private String oldStation;
    private String nextStation;
    private Duration timeToNextStation;
    private Integer availableSeats;
    private String finalDestination;
}
