package com.metrocarpool.gateway.dto;

import lombok.Builder;

@Builder
public class DriverSignUpRequestDTO {
    private String username;
    private String password;
    Long licenseId;
    private com.google.protobuf.Timestamp createdAt;
}
