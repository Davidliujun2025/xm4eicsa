package com.acme.aicslogin.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsAnyThreeOfFourCharacterCategories() {
        assertThat(policy.isValid("UpperLower123")).isTrue();
        assertThat(policy.isValid("UPPER123!@#$")).isTrue();
        assertThat(policy.isValid("lower123!@#$")).isTrue();
        assertThat(policy.isValid("UpperLower!@#")).isTrue();
    }

    @Test
    void rejectsInvalidLengthOrOnlyTwoCategories() {
        assertThat(policy.isValid("Aa1!short")).isFalse();
        assertThat(policy.isValid("OnlyLowercasePassword1!" )).isFalse();
        assertThat(policy.isValid("lowercase1234")).isFalse();
        assertThat(policy.isValid(null)).isFalse();
    }
}
