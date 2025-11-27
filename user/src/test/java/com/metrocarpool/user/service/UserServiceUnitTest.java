package com.metrocarpool.user.service;

import com.metrocarpool.user.entity.DriverEntity;
import com.metrocarpool.user.entity.RiderEntity;
import com.metrocarpool.user.repository.DriverRepository;
import com.metrocarpool.user.repository.RiderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceUnitTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private RiderRepository riderRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("driverSignUp - Should save and return driver")
    void driverSignUp_SavesAndReturnsDriver() {
        // Given
        String username = "testdriver";
        String password = "password123";
        Long licenseId = 12345L;
        
        DriverEntity savedDriver = DriverEntity.builder()
                .id(1L)
                .username(username)
                .password(password)
                .licenseId(licenseId)
                .build();
        
        when(driverRepository.save(any(DriverEntity.class))).thenReturn(savedDriver);

        // When
        DriverEntity result = userService.driverSignUp(username, password, licenseId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo(username);
        verify(driverRepository).save(any(DriverEntity.class));
    }

   @Test
    @DisplayName("riderSignUp - Should save and return rider")
    void riderSignUp_SavesAndReturnsRider() {
        // Given
        String username = "testrider";
        String password = "password123";
        
        RiderEntity savedRider = RiderEntity.builder()
                .id(1L)
                .username(username)
                .password(password)
                .build();
        
        when(riderRepository.save(any(RiderEntity.class))).thenReturn(savedRider);

        // When
        RiderEntity result = userService.riderSignUp(username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo(username);
        verify(riderRepository).save(any(RiderEntity.class));
    }

    @Test
    @DisplayName("driverLogin - Should return driver when credentials are correct")
    void driverLogin_ReturnsDriver_WhenCredentialsCorrect() {
        // Given
        String username = "testdriver";
        String password = "password123";
        
        DriverEntity driver = DriverEntity.builder()
                .id(1L)
                .username(username)
                .password(password)
                .licenseId(12345L)
                .build();
        
        when(driverRepository.findByUsername(username)).thenReturn(Optional.of(driver));

        // When
        DriverEntity result = userService.driverLogin(username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(driverRepository).findByUsername(username);
    }

    @Test
    @DisplayName("riderLogin - Should return rider when credentials are correct")
    void riderLogin_ReturnsRider_WhenCredentialsCorrect() {
        // Given
        String username = "testrider";
        String password = "password123";
        
        RiderEntity rider = RiderEntity.builder()
                .id(1L)
                .username(username)
                .password(password)
                .build();
        
        when(riderRepository.findByUsername(username)).thenReturn(Optional.of(rider));

        // When
        RiderEntity result = userService.riderLogin(username, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(riderRepository).findByUsername(username);
    }
}
