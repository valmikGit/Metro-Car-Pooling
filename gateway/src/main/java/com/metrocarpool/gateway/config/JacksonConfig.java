package com.metrocarpool.gateway.config;

import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  @Bean
  public ProtobufModule protobufModule() {
    return new ProtobufModule();
  }
}
