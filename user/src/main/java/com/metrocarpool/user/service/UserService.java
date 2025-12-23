package com.metrocarpool.user.service;

import com.metrocarpool.user.entity.DriverEntity;
import com.metrocarpool.user.entity.RiderEntity;
import com.metrocarpool.user.repository.DriverRepository;
import com.metrocarpool.user.repository.RiderRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Builder
@Slf4j
@RequiredArgsConstructor
public class UserService {
  private final DriverRepository driverRepository;
  private final RiderRepository riderRepository;

  public DriverEntity driverSignUp(String username, String password, Long licenseId) {
    try {
      log.info("Reached UserService.driverSignUp.");

      DriverEntity driverEntity =
          driverRepository.save(
              DriverEntity.builder()
                  .username(username)
                  .password(password)
                  .licenseId(licenseId)
                  .build());
      return driverEntity;
    } catch (Exception e) {
      log.error("Error in UserService.driverSignUp = {}", e.getMessage());
      return null;
    }
  }

  public RiderEntity riderSignUp(String username, String password) {
    try {
      log.info("Reached UserService.riderSignUp.");

      RiderEntity riderEntity =
          riderRepository.save(RiderEntity.builder().username(username).password(password).build());
      return riderEntity;
    } catch (Exception e) {
      log.error("Error in UserService.riderSignUp = {}", e.getMessage());
      return null;
    }
  }

  public DriverEntity driverLogin(String username, String password) {
    try {
      log.info("Reached UserService.driverLogin.");
      DriverEntity driverEntity = driverRepository.findByUsername(username).orElse(null);
      if (driverEntity == null) {
        return null;
      }
      if (password.equals(driverEntity.getPassword())) {
        return driverEntity;
      }
      return null;
    } catch (Exception e) {
      log.error("Error in UserService.driverLogin = {}", e.getMessage());
      return null;
    }
  }

  public RiderEntity riderLogin(String username, String password) {
    try {
      log.info("Reached UserService.riderLogin.");
      RiderEntity riderEntity = riderRepository.findByUsername(username).orElse(null);
      if (riderEntity == null) {
        return null;
      }
      if (password.equals(riderEntity.getPassword())) {
        return riderEntity;
      }
      return null;
    } catch (Exception e) {
      log.error("Error in UserService.riderLogin = {}", e.getMessage());
      return null;
    }
  }
}
