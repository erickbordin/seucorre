package com.seucorre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SeuCorreApplication {

    @Bean
    Clock applicationClock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SeuCorreApplication.class, args);
    }
}
