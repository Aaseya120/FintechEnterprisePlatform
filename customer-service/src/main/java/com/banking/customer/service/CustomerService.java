package com.banking.customer.service;

import com.banking.common.audit.BankingServiceRegistry;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerActionAuditLog;
import com.banking.customer.dto.CustomerDtos.*;
import com.banking.customer.repository.CustomerActionAuditRepository;
import com.banking.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;
    private final CustomerActionAuditRepository auditRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerActionAuditRepository auditRepository,
                           KafkaTemplate<String, Object> kafkaTemplate) {
        this.customerRepository = customerRepository;
        this.auditRepository = auditRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public CustomerResponseDto onboardCustomer(OnboardingRequestDto dto) {
        if (customerRepository.existsByEmail(dto.email())) {
            throw new BankingException("CUSTOMER_EXISTS", "A customer with email " + dto.email() + " already exists", HttpStatus.CONFLICT);
        }
        if (customerRepository.existsByPhone(dto.phone())) {
            throw new BankingException("PHONE_EXISTS", "A customer with phone " + dto.phone() + " already exists", HttpStatus.CONFLICT);
        }

        String id = UUID.randomUUID().toString();
        String customerNumber = "CUST-" + (10000000 + RANDOM.nextInt(90000000));

        Customer customer = new Customer(
                id,
                customerNumber,
                dto.firstName(),
                dto.lastName(),
                dto.email(),
                dto.phone(),
                dto.dateOfBirth(),
                dto.address()
        );

        if (dto.preferredTier() != null && !dto.preferredTier().isBlank()) {
            try {
                customer.upgradeTier(Customer.CustomerTier.valueOf(dto.preferredTier().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid preferred tier '{}' provided during onboarding, defaulting to BASIC", dto.preferredTier());
            }
        }

        Customer saved = customerRepository.save(customer);
        log.info("Successfully initiated digital onboarding for customer [{}] with Tier [{}]",
                customerNumber, saved.getCustomerTier());

        // Audit customer onboarding action
        logCustomerAction(
                saved.getId(),
                BankingServiceRegistry.ONBOARDING_KYC.getServiceId(),
                BankingServiceRegistry.ONBOARDING_KYC.getServiceName(),
                "CUSTOMER_ONBOARD_INITIATED",
                saved.getId(),
                "Customer profile initiated with tier: " + saved.getCustomerTier(),
                "DIGITAL_ONBOARDING_PORTAL",
                "127.0.0.1",
                "SUCCESS"
        );

        // Notify downstream via Kafka
        kafkaTemplate.send("banking.customer.onboarded", saved.getId(), saved)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish customer.onboarded event for customer {}: {}", saved.getId(), ex.getMessage());
                    }
                });

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomer(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        return mapToDto(customer);
    }

    @Transactional
    public CustomerResponseDto upgradeCustomerTier(String customerId, Customer.CustomerTier newTier) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        customer.upgradeTier(newTier);
        Customer saved = customerRepository.save(customer);

        logCustomerAction(
                customerId,
                BankingServiceRegistry.ONBOARDING_KYC.getServiceId(),
                BankingServiceRegistry.ONBOARDING_KYC.getServiceName(),
                "CUSTOMER_TIER_UPGRADED",
                customerId,
                "Customer upgraded to tier: " + newTier,
                "CUSTOMER_PORTAL",
                "127.0.0.1",
                "SUCCESS"
        );

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CustomerDashboardDto getDashboardServices(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        Customer.CustomerTier tier = customer.getCustomerTier();
        boolean isActive = (customer.getStatus() == Customer.CustomerStatus.ACTIVE);

        List<ServiceAccessInfoDto> services = new ArrayList<>();

        // 1. Core Accounts Ledger
        services.add(new ServiceAccessInfoDto(
                BankingServiceRegistry.ACCOUNT_LEDGER.getServiceId(),
                BankingServiceRegistry.ACCOUNT_LEDGER.getServiceName(),
                "Checking, Savings, and Multi-Currency Accounts ledger balances",
                isActive ? "GRANTED" : "PENDING_KYC",
                "BASIC",
                List.of("VIEW_BALANCE", "DOWNLOAD_STATEMENT", "OPEN_ACCOUNT")
        ));

        // 2. Fund Transfers
        services.add(new ServiceAccessInfoDto(
                BankingServiceRegistry.PAYMENT_TRANSFER.getServiceId(),
                BankingServiceRegistry.PAYMENT_TRANSFER.getServiceName(),
                "Internal Account-to-Account, Domestic NEFT/IMPS/UPI & Cards",
                isActive ? "GRANTED" : "PENDING_KYC",
                "BASIC",
                tier == Customer.CustomerTier.BASIC
                        ? List.of("INTERNAL_TRANSFER", "NEFT_TRANSFER", "IMPS_TRANSFER")
                        : List.of("INTERNAL_TRANSFER", "NEFT_TRANSFER", "IMPS_TRANSFER", "UPI_TRANSFER", "PRIORITY_PROCESSING")
        ));

        // 3. Card Management
        services.add(new ServiceAccessInfoDto(
                BankingServiceRegistry.CARD_MANAGEMENT.getServiceId(),
                BankingServiceRegistry.CARD_MANAGEMENT.getServiceName(),
                "Debit and Credit card issuance, PIN control, and spend limits",
                isActive ? "GRANTED" : "PENDING_KYC",
                "BASIC",
                tier.isInternationalAccess()
                        ? List.of("DOMESTIC_CARD", "INTERNATIONAL_CARD", "CONTACTLESS", "AIRPORT_LOUNGE_ACCESS")
                        : List.of("DOMESTIC_CARD", "CONTACTLESS")
        ));

        // 4. Forex & Cross-Border Remittance (Premium/Platinum/HNI feature)
        boolean fxGranted = isActive && (tier != Customer.CustomerTier.BASIC);
        services.add(new ServiceAccessInfoDto(
                BankingServiceRegistry.EXCHANGE_RATE.getServiceId(),
                BankingServiceRegistry.EXCHANGE_RATE.getServiceName(),
                "Real-time interbank foreign exchange rates and guaranteed quote locks",
                fxGranted ? "GRANTED" : "UPGRADE_REQUIRED",
                "PREMIUM",
                fxGranted
                        ? List.of("LIVE_FX_TICKER", "CROSS_BORDER_REMITTANCE", "PREFERENTIAL_FX_SPREAD")
                        : List.of("LIVE_FX_TICKER")
        ));

        // 5. Lending & Loans
        services.add(new ServiceAccessInfoDto(
                BankingServiceRegistry.LOAN_LENDING.getServiceId(),
                BankingServiceRegistry.LOAN_LENDING.getServiceName(),
                "Instant personal and commercial loan pre-approvals and amortization schedule",
                isActive ? "GRANTED" : "PENDING_KYC",
                "BASIC",
                List.of("LOAN_CALCULATOR", "APPLY_LOAN", "VIEW_SCHEDULE")
        ));

        // 6. Financial Reporting & Statements
        services.add(new ServiceAccessInfoDto(
                BankingServiceRegistry.REPORTING.getServiceId(),
                BankingServiceRegistry.REPORTING.getServiceName(),
                "Multi-format financial statement generation (PDF, Excel .xlsx, CSV, JSON)",
                isActive ? "GRANTED" : "PENDING_KYC",
                "BASIC",
                List.of("EXPORT_PDF", "EXPORT_EXCEL", "EXPORT_CSV", "EXPORT_JSON")
        ));

        return new CustomerDashboardDto(
                mapToDto(customer),
                tier.name(),
                tier.getDailyTransferLimit(),
                tier.getMaxCards(),
                tier.isInternationalAccess(),
                services
        );
    }

    @Transactional
    public CustomerActionAuditLog logCustomerAction(String customerId, String serviceId, String serviceName,
                                                    String actionType, String resourceId, String details,
                                                    String channel, String ipAddress, String status) {
        CustomerActionAuditLog audit = new CustomerActionAuditLog(
                UUID.randomUUID().toString(),
                customerId,
                serviceId,
                serviceName,
                actionType,
                resourceId,
                details,
                channel,
                ipAddress,
                status != null ? status : "SUCCESS",
                Instant.now()
        );
        return auditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<CustomerActionAuditLog> getCustomerAuditLogs(String customerId) {
        return auditRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }

    public CustomerResponseDto mapToDto(Customer c) {
        Customer.CustomerTier tier = c.getCustomerTier() != null ? c.getCustomerTier() : Customer.CustomerTier.BASIC;
        return new CustomerResponseDto(
                c.getId(),
                c.getCustomerNumber(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhone(),
                c.getDateOfBirth(),
                c.getAddress(),
                c.getRiskCategory(),
                c.getStatus(),
                tier,
                tier.getDailyTransferLimit(),
                tier.getMaxCards(),
                c.getCreatedAt()
        );
    }
}
