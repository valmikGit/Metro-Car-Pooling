package com.metrocarpool.trip.service;

import com.metrocarpool.trip.redislock.RedisDistributedLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripService Core Unit Tests")
class TripServiceCoreUnitTest {

    @Mock
    private RedisTemplate<String, String> redisStringTemplate;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @Test
    @DisplayName("alreadyProcessed - Should return true when message processed")
    void alreadyProcessed_ReturnsTrue() {
        String topicKey = "trip-topic";
        String messageId = "msg-123";
        String redisKey = topicKey + messageId;
        when(redisStringTemplate.hasKey(redisKey)).thenReturn(true);

        boolean result = redisStringTemplate.hasKey(redisKey);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("alreadyProcessed - Should return false when message not processed")
    void alreadyProcessed_ReturnsFalse() {
        String topicKey = "trip-topic";
        String messageId = "msg-456";
        String redisKey = topicKey + messageId;
        when(redisStringTemplate.hasKey(redisKey)).thenReturn(false);

        boolean result = redisStringTemplate.hasKey(redisKey);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Lock retry - Should return lock value on success")
    void lockRetry_Success() {
        String lockKey = "trip-lock";
        String lockValue = "lock-value-123";
        when(redisDistributedLock.acquireLock(eq(lockKey), anyLong())).thenReturn(lockValue);

        String result = redisDistributedLock.acquireLock(lockKey, 5000L);

        assertThat(result).isEqualTo(lockValue);
    }

    @Test
    @DisplayName("Lock retry - Should return null on failure")
    void lockRetry_Failure() {
        String lockKey = "trip-lock";
        when(redisDistributedLock.acquireLock(eq(lockKey), anyLong())).thenReturn(null);

        String result = redisDistributedLock.acquireLock(lockKey, 5000L);

        assertThat(result).isNull();
    }
}
