package com.carepilot.agentaccount.service;

import com.carepilot.agentaccount.dto.CreateAgentAccountRequest;
import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.repository.AgentAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    void createAccountShouldSaveAccountWithEncryptedPassword() {
        CreateAgentAccountRequest request = new CreateAgentAccountRequest();
        request.setName("LingWang");
        request.setPhone("13812345678");
        request.setEmail("LING@example.com");
        request.setInitialPassword("password1234");

        when(repository.findByPhone("13812345678"))
                .thenReturn(Optional.empty());
        when(repository.findByEmail("ling@example.com"))
                .thenReturn(Optional.empty());

        when(repository.save(any(AgentAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentAccount result = service.createAccount(request);

        ArgumentCaptor<AgentAccount> captor =
                ArgumentCaptor.forClass(AgentAccount.class);

        verify(repository).save(captor.capture());

        AgentAccount savedAccount = captor.getValue();

        assertEquals("LingWang", savedAccount.getName());
        assertEquals("13812345678", savedAccount.getPhone());
        assertEquals("ling@example.com", savedAccount.getEmail());
        assertEquals("CUSTOMER_SERVICE", savedAccount.getRole());
        assertEquals("ENABLED", savedAccount.getStatus());

        assertNotEquals(
                "password1234",
                savedAccount.getPasswordHash()
        );

        assertTrue(
                savedAccount.getPasswordHash().startsWith("$2")
        );

        assertSame(savedAccount, result);
    }
    @Test
    void createAccountShouldThrowExceptionWhenPhoneExists() {
        CreateAgentAccountRequest request = new CreateAgentAccountRequest();
        request.setName("LingWang");
        request.setPhone("13812345678");
        request.setEmail("ling@example.com");
        request.setInitialPassword("password1234");

        when(repository.findByPhone("13812345678"))
                .thenReturn(Optional.of(new AgentAccount()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAccount(request)
        );

        assertEquals("该手机号已存在", exception.getMessage());

        verify(repository, never()).save(any());
    }
    @Test
    void createAccountShouldThrowExceptionWhenEmailExists() {
        CreateAgentAccountRequest request = new CreateAgentAccountRequest();
        request.setName("LingWang");
        request.setPhone("13812345678");
        request.setEmail("ling@example.com");
        request.setInitialPassword("password1234");

        when(repository.findByPhone("13812345678"))
                .thenReturn(Optional.empty());

        when(repository.findByEmail("ling@example.com"))
                .thenReturn(Optional.of(new AgentAccount()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAccount(request)
        );

        assertEquals("该邮箱已存在", exception.getMessage());

        verify(repository, never()).save(any());
    }
    @Test
    void createAccountShouldThrowExceptionWhenPasswordEqualsPhone() {
        CreateAgentAccountRequest request = new CreateAgentAccountRequest();
        request.setName("LingWang");
        request.setPhone("13812345678");
        request.setEmail("ling@example.com");
        request.setInitialPassword("13812345678");

        when(repository.findByPhone("13812345678"))
                .thenReturn(Optional.empty());

        when(repository.findByEmail("ling@example.com"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAccount(request)
        );

        assertEquals(
                "初始密码不能与手机号完全一致",
                exception.getMessage()
        );

        verify(repository, never()).save(any());
    }
}