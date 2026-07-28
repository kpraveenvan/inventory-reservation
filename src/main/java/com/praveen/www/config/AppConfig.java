package com.praveen.www.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling and binds reservation configuration properties.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(ReservationProperties.class)
public class AppConfig {
}
