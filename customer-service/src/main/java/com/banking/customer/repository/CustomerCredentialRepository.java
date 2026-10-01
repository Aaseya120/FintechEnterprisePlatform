package com.banking.customer.repository;

import com.banking.customer.domain.CustomerCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerCredentialRepository extends JpaRepository<CustomerCredential, String> {

    Optional<CustomerCredential> findByUsername(String username);

    Optional<CustomerCredential> findByCustomerId(String customerId);

    Optional<CustomerCredential> findByRefreshToken(String refreshToken);
}
