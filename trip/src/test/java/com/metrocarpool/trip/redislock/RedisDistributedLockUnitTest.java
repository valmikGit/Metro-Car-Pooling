package com.metrocarpool.trip.redislock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Trip RedisDistributedLock Unit Tests")
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
        String lockKey = "test-lock";
        long timeout = 5000L;
        when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), eq(timeout), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        String lockValue = redisDistributedLock.acquireLock(lockKey, timeout);

        assertThat(lockValue).isNotNull();
    }

    @Test
    @DisplayName("acquireLock - Should return null when lock already held")
    void acquireLock_FailsWhenLockHeld() {
        String lockKey = "test-lock";
        long timeout = 5000L;
        when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), eq(timeout), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(false);

        String lockValue = redisDistributedLock.acquireLock(lockKey, timeout);

        assertThat(lockValue).isNull();
    }

    @Test
    @DisplayName("acquireLock - Should generate unique lock values")
    void acquireLock_GeneratesUniqueLockValues() {
        String lockKey = "test-lock";
        long timeout = 5000L;
        ArgumentCaptor<String> lockValueCaptor = ArgumentCaptor.forClass(String.class);
        
        when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), lockValueCaptor.capture(), eq(timeout), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        String lockValue1 = redisDistributedLock.acquireLock(lockKey, timeout);
        String lockValue2 = redisDistributedLock.acquireLock(lockKey, timeout);

        assertThat(lockValue1).isNotEqualTo(lockValue2);
    }
}
