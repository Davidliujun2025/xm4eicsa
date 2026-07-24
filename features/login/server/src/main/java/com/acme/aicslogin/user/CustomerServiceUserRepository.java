package com.acme.aicslogin.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerServiceUserRepository extends JpaRepository<CustomerServiceUser, Long> {
    Optional<CustomerServiceUser> findByAccount(String account);
}
