package com.carepilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CarePilotBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarePilotBackendApplication.class, args);
    }
}
