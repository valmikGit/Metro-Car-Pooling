package com.metrocarpool.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DriverSignUpRequestDTO {
  private String username;
  private String password;
  Long licenseId;
  //    private com.google.protobuf.Timestamp createdAt;
}
