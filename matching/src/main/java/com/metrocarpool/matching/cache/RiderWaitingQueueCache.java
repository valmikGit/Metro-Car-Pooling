package com.metrocarpool.matching.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RiderWaitingQueueCache {
    private Long riderId;
    private String pickUpStation;
    private Long arrivalTime;
    private String destinationPlace;
}
