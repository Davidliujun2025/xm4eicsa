package com.acme.aicslogin;

import com.acme.aicslogin.config.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties.class)
public class AiCustomerServiceLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCustomerServiceLoginApplication.class, args);
    }
}
