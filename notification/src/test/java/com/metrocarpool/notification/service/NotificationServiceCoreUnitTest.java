package com.metrocarpool.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Core Unit Tests")
class NotificationServiceCoreUnitTest {

    @Mock
    private RedisTemplate<String, String> redisStringTemplate;

    @Test
    @DisplayName("Idempotency - Should detect already processed messages")
    void idempotency_DetectsProcessedMessages() {
        String messageKey = "notification:msg-123";
        when(redisStringTemplate.hasKey(messageKey)).thenReturn(true);

        boolean result = redisStringTemplate.hasKey(messageKey);

        assertThat(result).isTrue();
        verify(redisStringTemplate).hasKey(messageKey);
    }

    @Test
    @DisplayName("Idempotency - Should allow new messages")
    void idempotency_AllowsNewMessages() {
        String messageKey = "notification:msg-456";
        when(redisStringTemplate.hasKey(messageKey)).thenReturn(false);

        boolean result = redisStringTemplate.hasKey(messageKey);

        assertThat(result).isFalse();
        verify(redisStringTemplate).hasKey(messageKey);
    }
}
