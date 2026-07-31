package com.acme.aicslogin.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus attribute) {
        return attribute == null ? null : attribute == UserStatus.ACTIVE ? "ENABLED" : attribute.name();
    }

    @Override
    public UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        return "ENABLED".equals(normalized) ? UserStatus.ACTIVE : UserStatus.valueOf(normalized);
    }
}
