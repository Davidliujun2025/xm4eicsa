package com.carepilot.agentaccount.controller;

import com.carepilot.agentaccount.exception.BusinessException;
import com.carepilot.agentaccount.exception.GlobalExceptionHandler;
import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.service.AgentAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentAccountController.class)
@Import(GlobalExceptionHandler.class)
class AgentAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentAccountService service;

    @Test
    void rejectsMissingName() throws Exception {
        assertFieldError(
                json("", "13800138000", null, "Password123!"),
                "name",
                "请输入用户姓名"
        );
    }

    @Test
    void rejectsMissingPhone() throws Exception {
        assertFieldError(
                json("Ling Wang", "", null, "Password123!"),
                "phone",
                "请输入手机号"
        );
    }

    @Test
    void rejectsMissingPassword() throws Exception {
        assertFieldError(
                json("Ling Wang", "13800138000", null, ""),
                "initialPassword",
                "请输入初始密码"
        );
    }

    @Test
    void rejectsInvalidPhone() throws Exception {
        assertFieldError(
                json("Ling Wang", "123", null, "Password123!"),
                "phone",
                "请输入正确的中国大陆11位手机号"
        );
    }

    @Test
    void rejectsInvalidEmailWhenProvided() throws Exception {
        assertFieldError(
                json("Ling Wang", "13800138000", "wrong", "Password123!"),
                "email",
                "请输入正确的邮箱地址"
        );
    }

    @Test
    void rejectsShortPassword() throws Exception {
        assertFieldError(
                json("Ling Wang", "13800138000", null, "Pass123"),
                "initialPassword",
                "初始密码必须为12～20位，包含字母和数字，仅支持字母、数字及 @、#、$、%、_、!"
        );
    }

    @Test
    void rejectsPasswordWithoutDigit() throws Exception {
        assertFieldError(
                json("Ling Wang", "13800138000", null, "PasswordOnly!"),
                "initialPassword",
                "初始密码必须为12～20位，包含字母和数字，仅支持字母、数字及 @、#、$、%、_、!"
        );
    }

    @Test
    void rejectsUnsupportedPasswordCharacter() throws Exception {
        assertFieldError(
                json("Ling Wang", "13800138000", null, "Password123&"),
                "initialPassword",
                "初始密码必须为12～20位，包含字母和数字，仅支持字母、数字及 @、#、$、%、_、!"
        );
    }

    @Test
    void returnsPhoneDuplicateCode() throws Exception {
        when(service.createAccount(any()))
                .thenThrow(new BusinessException(
                        "PHONE_ALREADY_EXISTS",
                        "该手机号已存在"
                ));

        mockMvc.perform(
                        post("/api/admin/customer-service-users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(
                                        "Ling Wang",
                                        "13800138000",
                                        null,
                                        "Password123!"
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("PHONE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message")
                        .value("该手机号已存在"));
    }

    @Test
    void returnsEmailDuplicateCode() throws Exception {
        when(service.createAccount(any()))
                .thenThrow(new BusinessException(
                        "EMAIL_ALREADY_EXISTS",
                        "该邮箱已存在"
                ));

        mockMvc.perform(
                        post("/api/admin/customer-service-users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(
                                        "Ling Wang",
                                        "13800138000",
                                        "ling@example.com",
                                        "Password123!"
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message")
                        .value("该邮箱已存在"));
    }

    @Test
    void createsAccountWithEmptyEmail() throws Exception {
        AgentAccount account = account();
        account.setEmail(null);

        when(service.createAccount(any()))
                .thenReturn(account);

        mockMvc.perform(
                        post("/api/admin/customer-service-users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(
                                        "Ling Wang",
                                        "13800138000",
                                        null,
                                        "Password123!"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$.data.email")
                        .doesNotExist());
    }

    @Test
    void createsAccountNormally() throws Exception {
        when(service.createAccount(any()))
                .thenReturn(account());

        mockMvc.perform(
                        post("/api/admin/customer-service-users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(
                                        "Ling Wang",
                                        "13800138000",
                                        "ling@example.com",
                                        "Password123!"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$.message")
                        .value("客服人员账号创建成功"))
                .andExpect(jsonPath("$.data.role")
                        .value("CUSTOMER_SERVICE"))
                .andExpect(jsonPath("$.data.status")
                        .value("ENABLED"));
    }

    private void assertFieldError(
            String content,
            String field,
            String message
    ) throws Exception {
        mockMvc.perform(
                        post("/api/admin/customer-service-users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath(
                        "$.data.fieldErrors." + field
                ).value(message));
    }

    private String json(
            String name,
            String phone,
            String email,
            String password
    ) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("phone", phone);
        request.put("email", email == null ? "" : email);
        request.put("initialPassword", password);

        return objectMapper.writeValueAsString(request);
    }

    private AgentAccount account() {
        AgentAccount account = new AgentAccount();

        account.setUserId(1L);
        account.setName("Ling Wang");
        account.setPhone("13800138000");
        account.setEmail("ling@example.com");
        account.setRole("CUSTOMER_SERVICE");
        account.setStatus("ENABLED");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        return account;
    }
}
