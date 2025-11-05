package com.metrocarpool.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.kafka.common.protocol.types.Field;

@Builder
@Getter
@Setter
public class DriverStatusResponseDTO {
    private Boolean status;
}
