package com.metrocarpool.matching.cache;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchingDriverCache {
  private Long driverId;
  private Duration timeToReachStation;
  private Integer availableSeats;
}
