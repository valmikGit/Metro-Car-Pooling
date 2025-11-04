package com.metrocarpool.gateway.dto;

import lombok.Builder;
import org.apache.kafka.common.protocol.types.Field;

@Builder
public class DriverStatusResponseDTO {
    private Boolean status;
}
