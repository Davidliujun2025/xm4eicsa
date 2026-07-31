package com.acme.aicslogin.user;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sys_user")
public class CustomerServiceUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String account;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password", length = 255)
    private String legacyPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 32)
    private RoleType roleType;

    @Convert(converter = UserStatusConverter.class)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected CustomerServiceUser() {
    }

    public static CustomerServiceUser create(
            String account,
            String passwordHash,
            String ignoredDisplayName,
            UserStatus status
    ) {
        CustomerServiceUser user = new CustomerServiceUser();
        user.account = account;
        user.passwordHash = passwordHash;
        user.status = status;
        user.roleType = RoleType.CUSTOMER_SERVICE;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getAccount() {
        return account;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getLegacyPassword() {
        return legacyPassword;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return account;
    }

    public UserStatus getStatus() {
        return status;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markLoggedIn(Instant instant) {
        this.lastLoginAt = instant;
    }
}
