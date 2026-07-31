package com.acme.aicslogin;

import com.acme.aicslogin.auth.AuthCookieService;
import com.acme.aicslogin.user.CustomerServiceUser;
import com.acme.aicslogin.user.CustomerServiceUserRepository;
import com.acme.aicslogin.user.UserStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class LoginFlowIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.auth.secure-cookies", () -> false);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerServiceUserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void prepareAccount() {
        userRepository.deleteAll();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
        userRepository.save(CustomerServiceUser.create(
                "demo.agent",
                passwordEncoder.encode("AiService2026!"),
                "演示客服",
                UserStatus.ACTIVE
        ));
    }

    @Test
    void completeLoginRefreshMeAndLogoutFlow() throws Exception {
        MvcResult configResult = mockMvc.perform(get("/api/v1/public/login-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandName").value("品牌名称"))
                .andExpect(jsonPath("$.data.forgotPasswordPath").value("/forgot-password"))
                .andExpect(jsonPath("$.data.passwordPolicy.minLength").value(12))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie csrf = configResult.getResponse().getCookie("XSRF-TOKEN");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json")
                        .content("""
                                {"account":"Demo.Agent","password":"AiService2026!","rememberMe":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.redirectPath").value("/workbench/"))
                .andExpect(jsonPath("$.data.user.account").value("demo.agent"))
                .andExpect(cookie().httpOnly(AuthCookieService.ACCESS_COOKIE, true))
                .andExpect(cookie().httpOnly(AuthCookieService.REFRESH_COOKIE, true))
                .andReturn();

        Cookie access = loginResult.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE);
        Cookie refresh = loginResult.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE);
        mockMvc.perform(get("/api/v1/auth/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("demo.agent"));

        MvcResult refreshedCsrfResult = mockMvc.perform(get("/api/v1/public/login-config"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie currentCsrf = refreshedCsrfResult.getResponse().getCookie("XSRF-TOKEN");

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(currentCsrf, refresh)
                        .header("X-XSRF-TOKEN", currentCsrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AuthCookieService.REFRESH_COOKIE))
                .andReturn();

        Cookie rotatedRefresh = refreshed.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(currentCsrf, refresh)
                        .header("X-XSRF-TOKEN", currentCsrf.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(currentCsrf, rotatedRefresh)
                        .header("X-XSRF-TOKEN", currentCsrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(AuthCookieService.ACCESS_COOKIE, 0))
                .andExpect(cookie().maxAge(AuthCookieService.REFRESH_COOKIE, 0));
    }

    @Test
    void returnsAcceptanceMessagesAndRejectsBlankInput() throws Exception {
        MvcResult configResult = mockMvc.perform(get("/api/v1/public/login-config")).andReturn();
        Cookie csrf = configResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json")
                        .content("""
                                {"account":"missing.agent","password":"AiService2026!","rememberMe":false}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("账号不存在，请找管理员"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json")
                        .content("""
                                {"account":"demo.agent","password":"WrongPassword1!","rememberMe":false}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("账号或密码不正确"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json")
                        .content("""
                                {"account":"","password":"","rememberMe":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT_REQUIRED"));
    }
}
