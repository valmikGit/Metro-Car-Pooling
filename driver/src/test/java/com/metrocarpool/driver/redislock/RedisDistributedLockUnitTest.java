package com.metrocarpool.driver.redislock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisDistributedLock Unit Tests")
class RedisDistributedLockUnitTest {

    @Mock
    private RedisTemplate<String, String> redisStringTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisDistributedLock redisDistributedLock;

    @BeforeEach
    void setUp() {
        redisDistributedLock = new RedisDistributedLock(redisStringTemplate);
    }

    @Test
    @DisplayName("acquireLock - Should successfully acquire lock")
    void acquireLock_Success() {
        // Given
        String lockKey = "test-lock";
        long timeout = 5000L;
        when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), eq(timeout), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        // When
        String lockValue = redisDistributedLock.acquireLock(lockKey, timeout);

        // Then
        assertThat(lockValue).isNotNull();
        assertThat(lockValue).isNotEmpty();
        verify(valueOperations).setIfAbsent(eq(lockKey), eq(lockValue), eq(timeout), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("acquireLock - Should return null when lock already held")
    void acquireLock_FailsWhenLockAlreadyHeld() {
        // Given
        String lockKey = "test-lock";
        long timeout = 5000L;
        when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), eq(timeout), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(false);

        // When
        String lockValue = redisDistributedLock.acquireLock(lockKey, timeout);

        // Then
        assertThat(lockValue).isNull();
        verify(valueOperations).setIfAbsent(eq(lockKey), anyString(), eq(timeout), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("releaseLock - Should successfully release lock with correct value")
    void releaseLock_Success() {
        // Given
        String lockKey = "test-lock";
        String lockValue = "my-lock-value";
        when(redisStringTemplate.execute(any(RedisCallback.class))).thenReturn(1L);

        // When
        boolean result = redisDistributedLock.releaseLock(lockKey, lockValue);

        // Then
        assertThat(result).isTrue();
        verify(redisStringTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("releaseLock - Should fail to release lock with incorrect value")
    void releaseLock_FailsWithIncorrectValue() {
        // Given
        String lockKey = "test-lock";
        String lockValue = "wrong-lock-value";
        when(redisStringTemplate.execute(any(RedisCallback.class))).thenReturn(0L);

        // When
        boolean result = redisDistributedLock.releaseLock(lockKey, lockValue);

        // Then
        assertThat(result).isFalse();
        verify(redisStringTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("releaseLock - Should handle null result from Lua script")
    void releaseLock_HandlesNullResult() {
        // Given
        String lockKey = "test-lock";
        String lockValue = "my-lock-value";
        when(redisStringTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        // When
        boolean result = redisDistributedLock.releaseLock(lockKey, lockValue);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("acquireLock - Should use unique lock values for different acquisitions")
    void acquireLock_GeneratesUniqueLockValues() {
        // Given
        String lockKey = "test-lock";
        long timeout = 5000L;
        when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), eq(timeout), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        // When
        String lockValue1 = redisDistributedLock.acquireLock(lockKey, timeout);
        String lockValue2 = redisDistributedLock.acquireLock(lockKey, timeout);

        // Then
        assertThat(lockValue1).isNotNull();
        assertThat(lockValue2).isNotNull();
        assertThat(lockValue1).isNotEqualTo(lockValue2);
    }

    @Test
    @DisplayName("acquireLock - Should use correct timeout value")
    void acquireLock_UsesCorrectTimeout() {
        // Given
        String lockKey = "test-lock";
        long timeout = 10000L;
        
        when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> timeUnitCaptor = ArgumentCaptor.forClass(TimeUnit.class);
        
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // When
        redisDistributedLock.acquireLock(lockKey, timeout);

        // Then
        verify(valueOperations).setIfAbsent(
                eq(lockKey), 
                anyString(), 
                timeoutCaptor.capture(), 
                timeUnitCaptor.capture()
        );
        
        assertThat(timeoutCaptor.getValue()).isEqualTo(timeout);
        assertThat(timeUnitCaptor.getValue()).isEqualTo(TimeUnit.MILLISECONDS);
    }
}
