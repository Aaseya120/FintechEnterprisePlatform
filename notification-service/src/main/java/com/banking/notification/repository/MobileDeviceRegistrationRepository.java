package com.banking.notification.repository;

import com.banking.notification.domain.MobileDeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MobileDeviceRegistrationRepository extends JpaRepository<MobileDeviceRegistration, String> {

    List<MobileDeviceRegistration> findByCustomerIdAndIsActiveTrue(String customerId);

    Optional<MobileDeviceRegistration> findByDeviceToken(String deviceToken);
}
