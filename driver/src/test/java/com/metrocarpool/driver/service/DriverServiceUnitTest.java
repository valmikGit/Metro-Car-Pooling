package com.metrocarpool.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import com.metrocarpool.driver.cache.DriverCache;
import com.metrocarpool.driver.redislock.RedisDistributedLock;
import com.metrocarpool.driver.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverService Unit Tests")
class DriverServiceUnitTest {

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplateNearby;

    @Mock
    private RedisTemplate<String, Object> redisTemplateLocationMap;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @Mock
    private RedisTemplate<String, String> redisStringTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ValueOperations<String, String> stringValueOperations;

    @Mock
    private Acknowledgment acknowledgment;

    private DriverService driverService;

    private static final String LOCK_VALUE = "test-lock-value";
    private static final String DRIVER_TOPIC = "driver-updates-test";
    private static final String RIDE_COMPLETION_TOPIC = "trip-completed-test";

    @BeforeEach
    void setUp() {
        driverService = new DriverService(
                kafkaTemplate,
                redisTemplate,
                redisTemplateNearby,
                redisTemplateLocationMap,
                redisDistributedLock,
                redisStringTemplate,
                objectMapper
        );

        // Set topic values via reflection
        ReflectionTestUtils.setField(driverService, "DRIVER_TOPIC", DRIVER_TOPIC);
        ReflectionTestUtils.setField(driverService, "RIDE_COMPLETION_TOPIC", RIDE_COMPLETION_TOPIC);

        // Setup common mocks - using lenient() to avoid unnecessary stubbing errors
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisStringTemplate.opsForValue()).thenReturn(stringValueOperations);
    }

    @Test
    @DisplayName("processDriverInfo - Should successfully process valid driver info")
    void processDriverInfo_Success() throws Exception {
        // Given
        Long driverId = 1L;
        List<String> routePlaces = Arrays.asList("LocationA", "LocationB", "LocationC");
        String finalDestination = "LocationC";
        Integer availableSeats = 3;

        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(LOCK_VALUE);
        when(valueOperations.get("drivers")).thenReturn(new HashMap<Long, DriverCache>());
        when(stringValueOperations.get("location-location-map")).thenReturn(buildLocationMapJson());
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(TestDataBuilder.buildLocationLocationMap());

        // When
        boolean result = driverService.processDriverInfo(driverId, routePlaces, finalDestination, availableSeats);

        // Then
        assertThat(result).isTrue();
        verify(redisDistributedLock, atLeastOnce()).acquireLock(anyString(), eq(5000L));
        verify(valueOperations).set(eq("drivers"), any(Map.class));
        verify(redisDistributedLock, atLeastOnce()).releaseLock(anyString(), eq(LOCK_VALUE));
    }

    @Test
    @DisplayName("processDriverInfo - Should fail with invalid input (null driverId)")
    void processDriverInfo_FailsWithNullDriverId() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(LOCK_VALUE);

        // When
        boolean result = driverService.processDriverInfo(null, Arrays.asList("A", "B"), "B", 3);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations, never()).set(anyString(), any());
        verify(redisDistributedLock).releaseLock(anyString(), eq(LOCK_VALUE));
    }

    @Test
    @DisplayName("processDriverInfo - Should fail with invalid route (less than 2 places)")
    void processDriverInfo_FailsWithInvalidRoute() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(LOCK_VALUE);

        // When
        boolean result = driverService.processDriverInfo(1L, Arrays.asList("A"), "A", 3);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations, never()).set(anyString(), any());
    }

    @Test
    @DisplayName("processDriverInfo - Should fail when lock cannot be acquired")
    void processDriverInfo_FailsWhenLockNotAcquired() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(null);

        // When
        boolean result = driverService.processDriverInfo(1L, Arrays.asList("A", "B"), "B", 3);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations, never()).set(anyString(), any());
        verify(redisDistributedLock, never()).releaseLock(anyString(), anyString());
    }

    @Test
    @DisplayName("matchFoundUpdateCache - Should decrement available seats when match found")
    void matchFoundUpdateCache_Success() throws Exception {
        // Given
        Long driverId = 1L;
        Long riderId = 100L;
        String pickupStation = "Station1";
        
        DriverRiderMatchEvent event = TestDataBuilder.buildDriverRiderMatchEvent(driverId, riderId, pickupStation);
        byte[] message = event.toByteArray();

        Map<Long, DriverCache> driverCacheMap = new HashMap<>();
        DriverCache driverCache = TestDataBuilder.buildDriverCache(driverId, 3);
        driverCacheMap.put(driverId, driverCache);

        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(LOCK_VALUE);
        when(redisStringTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get("drivers")).thenReturn(driverCacheMap);

        // When
        driverService.matchFoundUpdateCache(message, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verify(stringValueOperations).set(contains("match_found_processed_kafka_msg"), eq("1"), eq(24L), any());
        
        ArgumentCaptor<Map> cacheCaptor = ArgumentCaptor.forClass(Map.class);
        verify(valueOperations).set(eq("drivers"), cacheCaptor.capture());
        
        Map<Long, DriverCache> updatedCache = cacheCaptor.getValue();
        assertThat(updatedCache.get(driverId).getAvailableSeats()).isEqualTo(2);
        
        verify(redisDistributedLock).releaseLock(anyString(), eq(LOCK_VALUE));
    }

    @Test
    @DisplayName("matchFoundUpdateCache - Should handle duplicate messages (idempotency)")
    void matchFoundUpdateCache_ShouldSkipDuplicateMessages() throws Exception {
        // Given
        DriverRiderMatchEvent event = TestDataBuilder.buildDriverRiderMatchEvent(1L, 100L, "Station1");
        byte[] message = event.toByteArray();

        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(LOCK_VALUE);
        when(redisStringTemplate.hasKey(anyString())).thenReturn(true); // Already processed

        // When
        driverService.matchFoundUpdateCache(message, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verify(valueOperations, never()).set(anyString(), any());
        verify(redisDistributedLock).releaseLock(anyString(), eq(LOCK_VALUE));
    }

    @Test
    @DisplayName("matchFoundUpdateCache - Should not go below 0 seats")
    void matchFoundUpdateCache_ShouldNotGoBelowZero() throws Exception {
        // Given
        Long driverId = 1L;
        DriverRiderMatchEvent event = TestDataBuilder.buildDriverRiderMatchEvent(driverId, 100L, "Station1");
        byte[] message = event.toByteArray();

        Map<Long, DriverCache> driverCacheMap = new HashMap<>();
        DriverCache driverCache = TestDataBuilder.buildDriverCache(driverId, 1); // Only 1 seat
        driverCacheMap.put(driverId, driverCache);

        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(LOCK_VALUE);
        when(redisStringTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get("drivers")).thenReturn(driverCacheMap);

        // When
        driverService.matchFoundUpdateCache(message, acknowledgment);

        // Then
        ArgumentCaptor<Map> cacheCaptor = ArgumentCaptor.forClass(Map.class);
        verify(valueOperations).set(eq("drivers"), cacheCaptor.capture());
        
        Map<Long, DriverCache> updatedCache = cacheCaptor.getValue();
        assertThat(updatedCache.get(driverId).getAvailableSeats()).isEqualTo(0);
    }

    @Test
    @DisplayName("matchFoundUpdateCache - Should handle missing driver gracefully")
    void matchFoundUpdateCache_ShouldHandleMissingDriver() throws Exception {
        // Given
        DriverRiderMatchEvent event = TestDataBuilder.buildDriverRiderMatchEvent(999L, 100L, "Station1");
        byte[] message = event.toByteArray();

        Map<Long, DriverCache> driverCacheMap = new HashMap<>();

        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(LOCK_VALUE);
        when(redisStringTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get("drivers")).thenReturn(driverCacheMap);

        // When
        driverService.matchFoundUpdateCache(message, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verify(valueOperations, never()).set(eq("drivers"), any());
    }

    @Test
    @DisplayName("matchFoundUpdateCache - Should handle lock acquisition failure")
    void matchFoundUpdateCache_ShouldHandleLockFailure() throws Exception {
        // Given
        DriverRiderMatchEvent event = TestDataBuilder.buildDriverRiderMatchEvent(1L, 100L, "Station1");
        byte[] message = event.toByteArray();

        when(redisDistributedLock.acquireLock(anyString(), anyLong())).thenReturn(null);

        // When
        driverService.matchFoundUpdateCache(message, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verify(valueOperations, never()).get(anyString());
        verify(redisDistributedLock, never()).releaseLock(anyString(), anyString());
    }

    // Helper methods
    private String buildLocationMapJson() {
        return """
                {
                    "LocationA": {"LocationB": 50.0, "LocationC": 100.0},
                    "LocationB": {"LocationC": 50.0}
                }
                """;
    }
}
