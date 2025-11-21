package com.metrocarpool.driver.redislock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedLock {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisDistributedLock(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Try to acquire a lock using SETNX + expiration.
     *
     * @param lockKey   The Redis key representing the lock.
     * @param timeoutMs Expiration time in milliseconds.
     * @return A unique lock value (UUID) if acquired, otherwise null.
     */
    public String acquireLock(String lockKey, long timeoutMs) {
        String lockValue = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS);

        return Boolean.TRUE.equals(success) ? lockValue : null;
    }

    /**
     * Atomic lock release using Lua script to ensure only the owner releases the lock.
     *
     * @param lockKey   The Redis key representing the lock.
     * @param lockValue The lock value that must match.
     * @return true if lock is released, false otherwise.
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "   return redis.call('del', KEYS[1]) " +
                        "else " +
                        "   return 0 " +
                        "end";

        byte[] result = redisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.eval(
                        script.getBytes(StandardCharsets.UTF_8),
                        ReturnType.INTEGER,
                        1,
                        lockKey.getBytes(StandardCharsets.UTF_8),
                        lockValue.getBytes(StandardCharsets.UTF_8)
                )
        );

        return result != null && result.length > 0 && result[0] == 1;
    }
}
