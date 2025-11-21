package com.metrocarpool.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SignUpOrLoginResponseDTO {
    private int STATUS_CODE;
}
