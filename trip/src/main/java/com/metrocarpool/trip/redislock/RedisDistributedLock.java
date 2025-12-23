package com.metrocarpool.trip.redislock;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisDistributedLock {

  private final RedisTemplate<String, String> redisStringTemplate;

  @Autowired
  public RedisDistributedLock(RedisTemplate<String, String> redisStringTemplate) {
    this.redisStringTemplate = redisStringTemplate;
  }

  /** Try to acquire a lock using SETNX + expiration. */
  public String acquireLock(String lockKey, long timeoutMs) {
    String lockValue = UUID.randomUUID().toString();

    Boolean success =
        redisStringTemplate
            .opsForValue()
            .setIfAbsent(lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS);

    return Boolean.TRUE.equals(success) ? lockValue : null;
  }

  /** Atomic lock release using a Lua script. */
  public boolean releaseLock(String lockKey, String lockValue) {
    String script =
        "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "   return redis.call('del', KEYS[1]) "
            + "else "
            + "   return 0 "
            + "end";

    Long result =
        redisStringTemplate.execute(
            (RedisCallback<Long>)
                connection ->
                    connection.eval(
                        script.getBytes(StandardCharsets.UTF_8),
                        ReturnType.INTEGER,
                        1,
                        lockKey.getBytes(StandardCharsets.UTF_8),
                        lockValue.getBytes(StandardCharsets.UTF_8)));

    return result != null && result == 1;
  }
}
