package com.carepilot.platform;

import com.acme.aicslogin.AiCustomerServiceLoginApplication;
import com.acme.aicslogin.config.AuthProperties;
import com.carepilot.CarePilotBackendApplication;
import com.carepilot.agentaccount.AgentAccountApplication;
import com.carepilot.chatworkbench.ChatWorkbenchApplication;
import com.carepilot.forbiddenwords.ForbiddenWordsApplication;
import com.carepilot.passwordreset.config.PasswordResetProperties;
import com.example.aics.AiCustomerServiceApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties({AuthProperties.class, PasswordResetProperties.class})
@ComponentScan(
        basePackages = {"com.acme.aicslogin", "com.carepilot", "com.example.aics"},
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        AiCustomerServiceLoginApplication.class,
                        CarePilotBackendApplication.class,
                        AgentAccountApplication.class,
                        ForbiddenWordsApplication.class,
                        ChatWorkbenchApplication.class,
                        AiCustomerServiceApplication.class,
                        Xm4eicsaApplication.class
                }))
@EntityScan(basePackages = {
        "com.acme.aicslogin",
        "com.carepilot.chatworkbench",
        "com.example.aics"
})
@EnableJpaRepositories(basePackages = {
        "com.acme.aicslogin",
        "com.carepilot.chatworkbench",
        "com.example.aics"
})
public class Xm4eicsaApplication {
    public static void main(String[] args) {
        SpringApplication.run(Xm4eicsaApplication.class, args);
    }
}
