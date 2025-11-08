package com.metrocarpool.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserLoginDTO {
    private String username;
    private String password;
//    private UserType userType;
}
