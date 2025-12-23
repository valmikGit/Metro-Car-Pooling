package com.metrocarpool.trip.configuration;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConfig {

  @Bean
  public KafkaAdmin kafkaAdmin(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    return new KafkaAdmin(configs);
  }

  @Bean
  public NewTopic driverRideCompletionTopic(
      @Value("${kafka.topics.driver-ride-completion}") String topicName) {
    return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic riderRideCompletionTopic(
      @Value("${kafka.topics.rider-ride-completion}") String topicName) {
    return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic driverLocationForRiderTopic(
      @Value("${kafka.topics.driver-location-rider}") String topicName) {
    return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
  }
}
