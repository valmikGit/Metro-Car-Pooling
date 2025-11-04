package com.metrocarpool.gateway.dto;

import lombok.Builder;

@Builder
public class RiderSignUpRequestDTO {
    private String username;
    private String password;
    private com.google.protobuf.Timestamp createdAt;
}
