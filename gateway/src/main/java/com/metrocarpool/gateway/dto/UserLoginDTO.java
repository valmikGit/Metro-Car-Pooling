package com.metrocarpool.gateway.dto;

import com.metrocarpool.gateway.enums.UserType;
import lombok.Builder;

@Builder
public class UserLoginDTO {
    private String username;
    private String password;
    private UserType userType;
}
