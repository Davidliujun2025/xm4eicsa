package com.carepilot.passwordreset.service;

import com.carepilot.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void acceptsPasswordWithAtLeastThreeCategories() {
        assertDoesNotThrow(() -> passwordPolicy.validate("NewPassword123!", "NewPassword123!"));
    }

    @Test
    void rejectsPasswordWhenConfirmationDoesNotMatch() {
        assertThrows(BusinessException.class,
                () -> passwordPolicy.validate("NewPassword123!", "OtherPassword123!"));
    }

    @Test
    void rejectsPasswordWhenTooShort() {
        assertThrows(BusinessException.class,
                () -> passwordPolicy.validate("Aa123!", "Aa123!"));
    }

    @Test
    void rejectsPasswordWhenCategoriesAreNotEnough() {
        assertThrows(BusinessException.class,
                () -> passwordPolicy.validate("passwordonlytext", "passwordonlytext"));
    }
}
