package com.metrocarpool.driver.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use JSON serialization for values, String for keys
        StringRedisSerializer str = new StringRedisSerializer();
        // Configure ObjectMapper to support Java Time (Duration, Instant, etc.)
        ObjectMapper om = new ObjectMapper();
        // Discover and register modules if available (e.g., JavaTimeModule)
        om.findAndRegisterModules();
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(om);
        template.setKeySerializer(str);
        template.setValueSerializer(json);
        template.setHashKeySerializer(str);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();

        return template;
    }

    // Additional template to read/write raw string values for tolerant parsing of legacy/plain JSON
    @Bean
    public RedisTemplate<String, String> redisStringTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        StringRedisSerializer str = new StringRedisSerializer();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(str);
        template.setValueSerializer(str);
        template.afterPropertiesSet();
        return template;
    }
}
