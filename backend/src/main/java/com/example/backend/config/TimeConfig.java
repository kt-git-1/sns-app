package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public Clock systemClock() {
        // すべてUTC基準で扱う（JWTやDBの時刻の一貫性を保つ）
        return Clock.systemUTC();
    }
}
