package com.metrocarpool.matching.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use JSON serialization for values, String for keys
        // Use JSON serialization for values, String for keys
        template.setKeySerializer(new StringRedisSerializer());
        
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        // initialize serializers / internal fields
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