package com.acme.aicslogin.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRulesTest {

    @Test
    void normalizesWhitespaceAndCase() {
        assertThat(AccountRules.normalize("  Agent.Name_01  ")).isEqualTo("agent.name_01");
    }

    @Test
    void acceptsConfiguredEmployeeAccountFormat() {
        assertThat(AccountRules.isValid("a-01_b.c")).isTrue();
    }

    @Test
    void rejectsTooShortWhitespaceAndUnsupportedCharacters() {
        assertThat(AccountRules.isValid("abc")).isFalse();
        assertThat(AccountRules.isValid("agent name")).isFalse();
        assertThat(AccountRules.isValid("客服001")).isFalse();
    }
}
