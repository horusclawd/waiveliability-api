package com.waiveliability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WaiveLiabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaiveLiabilityApplication.class, args);
    }
}
