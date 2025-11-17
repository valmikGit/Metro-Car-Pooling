package com.metrocarpool.gateway.controller;

import com.metrocarpool.gateway.client.UserGrpcClient;
import com.metrocarpool.gateway.dto.DriverSignUpRequestDTO;
import com.metrocarpool.gateway.dto.RiderSignUpRequestDTO;
import com.metrocarpool.gateway.dto.SignUpOrLoginResponseDTO;
import com.metrocarpool.gateway.dto.UserLoginDTO;
import com.metrocarpool.gateway.security.JwtConstant;
import com.metrocarpool.gateway.security.JwtUtil;
import com.metrocarpool.user.proto.SignUpOrLoginResponse;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Builder
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserGrpcClient userGrpcClient;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/add-driver")
    public SignUpOrLoginResponseDTO addDriver(@RequestBody DriverSignUpRequestDTO driverSignUpRequestDTO) {
        log.info("Reached RiderController.addDriver");
        log.info("Driver signup: Username = {}", driverSignUpRequestDTO.getUsername());
        return SignUpOrLoginResponseDTO.builder()
                .STATUS_CODE(userGrpcClient.DriverSignUpReq(driverSignUpRequestDTO).getSTATUSCODE())
                .build();
    }

    @PostMapping("/add-rider")
    public SignUpOrLoginResponseDTO addRider(@RequestBody RiderSignUpRequestDTO riderSignUpRequestDTO) {
        log.info("Reached RiderController.addRider");
        log.info("Rider signup: Username = {}", riderSignUpRequestDTO.getUsername());
        return SignUpOrLoginResponseDTO.builder()
                .STATUS_CODE(userGrpcClient.RiderSignUpReq(riderSignUpRequestDTO).getSTATUSCODE())
                .build();
    }

    @PostMapping("/login-driver")
    public ResponseEntity<?> loginDriver(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("Reached RiderController.loginDriver");
        log.info("Driver login: Username = {}", userLoginDTO.getUsername());
        SignUpOrLoginResponse signUpOrLoginResponse = userGrpcClient.DriverLoginReq(userLoginDTO);
        if (signUpOrLoginResponse.getSTATUSCODE() == 200) {
            // Generate JWT with "username" claim
            String token = jwtUtil.generateToken(userLoginDTO.getUsername());

            log.info("Received sign up or login response = {}",  signUpOrLoginResponse);

            // Return token in both body and Authorization header
            return ResponseEntity.ok()
                    .header(JwtConstant.JWT_HEADER, "Bearer " + token)
                    .body(Map.of("token", token));
        } else {
            log.info("Received sign up or login response = {}", signUpOrLoginResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(signUpOrLoginResponse);
        }
    }

    @PostMapping("/login-rider")
    public ResponseEntity<?> loginRider(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("Reached RiderController.loginRider");
        log.info("Rider login: Username = {}", userLoginDTO.getUsername());
        SignUpOrLoginResponse signUpOrLoginResponse = userGrpcClient.RiderLoginReq(userLoginDTO);
        if (signUpOrLoginResponse.getSTATUSCODE() == 200) {
            // Generate JWT with "username" claim
            String token = jwtUtil.generateToken(userLoginDTO.getUsername());

            log.info("Received sign up or login response = {}",  signUpOrLoginResponse);

            // Return token in both body and Authorization header
            return ResponseEntity.ok()
                    .header(JwtConstant.JWT_HEADER, "Bearer " + token)
                    .body(Map.of("token", token));
        } else {
            log.info("Received sign up or login response = {}", signUpOrLoginResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(signUpOrLoginResponse);
        }
    }
}