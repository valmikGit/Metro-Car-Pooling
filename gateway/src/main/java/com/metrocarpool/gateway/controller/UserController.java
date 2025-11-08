package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.UserGrpcClient;
import com.metrocarpool.gateway.dto.DriverSignUpRequestDTO;
import com.metrocarpool.gateway.dto.RiderSignUpRequestDTO;
import com.metrocarpool.gateway.dto.SignUpOrLoginResponseDTO;
import com.metrocarpool.gateway.dto.UserLoginDTO;
import com.metrocarpool.gateway.security.JwtUtil;
import com.metrocarpool.user.proto.SignUpOrLoginResponse;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@Builder
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserGrpcClient userGrpcClient;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping(value = "/add-driver")
    public SignUpOrLoginResponseDTO addDriver(@RequestBody DriverSignUpRequestDTO driverSignUpRequestDTO) {
        return SignUpOrLoginResponseDTO.builder()
                .STATUS_CODE(userGrpcClient.DriverSignUpReq(driverSignUpRequestDTO).getSTATUSCODE())
                .build();
    }

    @PostMapping(value = "/add-rider")
    public SignUpOrLoginResponseDTO addRider(@RequestBody RiderSignUpRequestDTO riderSignUpRequestDTO) {
        return SignUpOrLoginResponseDTO.builder()
                .STATUS_CODE(userGrpcClient.RiderSignUpReq(riderSignUpRequestDTO).getSTATUSCODE())
                .build();
    }

    @PostMapping(value = "/login-driver")
    public ResponseEntity<?> loginDriver(@RequestBody UserLoginDTO userLoginDTO) {
        SignUpOrLoginResponse signUpOrLoginResponse = userGrpcClient.DriverLoginReq(userLoginDTO);
        if (signUpOrLoginResponse.getSTATUSCODE() == 200) {
            String token = jwtUtil.generateToken(userLoginDTO.getUsername());
            return ResponseEntity.ok(Map.of("token", token));
        }  else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(signUpOrLoginResponse);
        }
    }

    @PostMapping(value = "/login-rider")
    public ResponseEntity<?> loginRider(@RequestBody UserLoginDTO userLoginDTO) {
        SignUpOrLoginResponse signUpOrLoginResponse = userGrpcClient.RiderLoginReq(userLoginDTO);
        if (signUpOrLoginResponse.getSTATUSCODE() == 200) {
            String token = jwtUtil.generateToken(userLoginDTO.getUsername());
            return ResponseEntity.ok(Map.of("token", token));
        }  else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(signUpOrLoginResponse);
        }
    }
}
