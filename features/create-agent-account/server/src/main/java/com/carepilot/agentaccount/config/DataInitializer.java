package com.carepilot.agentaccount.config;


import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.repository.AgentAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Configuration
public class DataInitializer {


    @Bean
    public CommandLineRunner initDatabase(
            AgentAccountRepository repository
    ) {

        return args -> {


            // 检查是否已经存在系统管理员
            long adminCount =
                    repository.countByRole("SYSTEM_ADMIN");


            // 已经存在管理员，不重复创建
            if (adminCount > 0) {
                return;
            }


            BCryptPasswordEncoder encoder =
                    new BCryptPasswordEncoder();


            // 创建默认管理员
            AgentAccount admin = new AgentAccount();


            admin.setName("陈一冉");

            admin.setPhone("13800000000");

            admin.setEmail("admin@carepilot.com");


            // 默认登录密码：123456
            admin.setPasswordHash(
                    encoder.encode("123456")
            );


            admin.setRole(
                    "SYSTEM_ADMIN"
            );


            admin.setStatus(
                    "ENABLED"
            );


            repository.save(admin);


            System.out.println(
                    "初始化系统管理员成功"
            );

        };
    }
}