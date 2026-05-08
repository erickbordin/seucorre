package com.seucorre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SeuCorreApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeuCorreApplication.class, args);
    }
}
