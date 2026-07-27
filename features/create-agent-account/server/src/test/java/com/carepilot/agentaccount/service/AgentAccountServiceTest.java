package com.carepilot.agentaccount.service;

import com.carepilot.agentaccount.dto.CreateAgentAccountRequest;
import com.carepilot.agentaccount.exception.BusinessException;
import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.repository.AgentAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentAccountServiceTest {
    private AgentAccountRepository repository;
    private AgentAccountService service;

    @BeforeEach
    void setUp() {
        repository = mock(AgentAccountRepository.class);
        service = new AgentAccountService(repository);
    }

    @Test
    void createAccountShouldSaveEncryptedPasswordAndNormalizeEmail() {
        CreateAgentAccountRequest request = request("Ling Wang", "13812345678", "LING@example.com", "Password123!");
        when(repository.findByPhone("13812345678")).thenReturn(Optional.empty());
        when(repository.findByEmail("ling@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            AgentAccount a = invocation.getArgument(0);
            a.setUserId(1L); a.setCreatedAt(LocalDateTime.now()); a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        AgentAccount result = service.createAccount(request);
        ArgumentCaptor<AgentAccount> captor = ArgumentCaptor.forClass(AgentAccount.class);
        verify(repository).save(captor.capture());
        AgentAccount saved = captor.getValue();
        assertEquals("Ling Wang", saved.getName());
        assertEquals("ling@example.com", saved.getEmail());
        assertEquals("CUSTOMER_SERVICE", saved.getRole());
        assertEquals("ENABLED", saved.getStatus());
        assertNotEquals("Password123!", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        verify(repository).saveOperationLog(1L, "CREATE", "创建客服人员账号");
        assertSame(saved, result);
    }

    @Test
    void createAccountAllowsEmptyEmailAndSkipsEmailLookup() {
        CreateAgentAccountRequest request = request("Ling Wang", "13812345678", "   ", "Password123!");
        when(repository.findByPhone("13812345678")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> { AgentAccount a = invocation.getArgument(0); a.setUserId(1L); return a; });
        AgentAccount result = service.createAccount(request);
        assertNull(result.getEmail());
        verify(repository, never()).findByEmail(anyString());
    }

    @Test
    void createAccountRejectsExistingPhone() {
        when(repository.findByPhone("13812345678")).thenReturn(Optional.of(new AgentAccount()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createAccount(request("Ling Wang", "13812345678", null, "Password123!")));
        assertEquals("PHONE_ALREADY_EXISTS", ex.getCode());
        assertEquals("该手机号已存在", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createAccountRejectsExistingEmail() {
        when(repository.findByPhone("13812345678")).thenReturn(Optional.empty());
        when(repository.findByEmail("ling@example.com")).thenReturn(Optional.of(new AgentAccount()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createAccount(request("Ling Wang", "13812345678", "ling@example.com", "Password123!")));
        assertEquals("EMAIL_ALREADY_EXISTS", ex.getCode());
        assertEquals("该邮箱已存在", ex.getMessage());
    }

    @Test
    void createAccountRejectsPasswordEqualToPhone() {
        when(repository.findByPhone("13812345678")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createAccount(request("Ling Wang", "13812345678", null, "13812345678")));
        assertEquals("PASSWORD_EQUALS_PHONE", ex.getCode());
    }

    private CreateAgentAccountRequest request(String name, String phone, String email, String password) {
        CreateAgentAccountRequest r = new CreateAgentAccountRequest();
        r.setName(name); r.setPhone(phone); r.setEmail(email); r.setInitialPassword(password); return r;
    }
}
