package com.carepilot.tokenquota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TokenQuotaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenQuotaBackendApplication.class, args);
    }
}
