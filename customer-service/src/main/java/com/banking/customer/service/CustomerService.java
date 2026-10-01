package com.banking.customer.service;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.customer.domain.Customer;
import com.banking.customer.dto.CustomerDtos.CustomerResponseDto;
import com.banking.customer.dto.CustomerDtos.OnboardingRequestDto;
import com.banking.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CustomerService(CustomerRepository customerRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.customerRepository = customerRepository;
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

        Customer saved = customerRepository.save(customer);
        log.info("Successfully initiated digital onboarding for customer [{}]", customerNumber);

        // Notify downstream via Kafka
        kafkaTemplate.send("banking.customer.onboarded", saved.getId(), saved);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomer(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        return mapToDto(customer);
    }

    public CustomerResponseDto mapToDto(Customer c) {
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
                c.getCreatedAt()
        );
    }
}
