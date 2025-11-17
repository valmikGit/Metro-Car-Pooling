package com.metrocarpool.user.service;

import com.metrocarpool.user.entity.DriverEntity;
import com.metrocarpool.user.entity.RiderEntity;
import com.metrocarpool.user.repository.DriverRepository;
import com.metrocarpool.user.repository.RiderRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.SecurityConfig;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Builder
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final DriverRepository driverRepository;
    private final RiderRepository riderRepository;

    public Boolean driverSignUp(String username, String password, Long licenseId){
        try {
            log.info("Reached UserService.driverSignUp.");

            driverRepository.save(
                    DriverEntity.builder()
                    .username(username)
                    .password(password)
                    .licenseId(licenseId)
                    .build()
            );
            return true;
        }
        catch (Exception e){
            log.error("Error in UserService.driverSignUp = {}", e.getMessage());
            return false;
        }
    }

    public Boolean riderSignUp(String username, String password){
        try {
            log.info("Reached UserService.riderSignUp.");

            riderRepository.save(
                    RiderEntity.builder()
                            .username(username)
                            .password(password)
                            .build()
            );
            return true;
        }
        catch (Exception e){
            log.error("Error in UserService.riderSignUp = {}", e.getMessage());
            return false;
        }
    }

    public Boolean driverLogin(String username, String password) {
        try {
            log.info("Reached UserService.driverLogin.");
            DriverEntity driverEntity = driverRepository.findByUsername(username).orElse(null);
            if (driverEntity == null) {
                return false;
            }
            return password.equals(driverEntity.getPassword());
        } catch (Exception e) {
            log.error("Error in UserService.driverLogin = {}", e.getMessage());
            return false;
        }
    }

    public Boolean riderLogin(String username, String password) {
        try {
            log.info("Reached UserService.riderLogin.");
            RiderEntity riderEntity =  riderRepository.findByUsername(username).orElse(null);
            if (riderEntity == null) {
                return false;
            }
            return password.equals(riderEntity.getPassword());
        } catch (Exception e) {
            log.error("Error in UserService.riderLogin = {}", e.getMessage());
            return false;
        }
    }
}
