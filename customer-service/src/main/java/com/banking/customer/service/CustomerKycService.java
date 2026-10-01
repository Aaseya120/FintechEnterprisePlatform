package com.banking.customer.service;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerKyc;
import com.banking.customer.dto.CustomerDtos.KycReviewDto;
import com.banking.customer.dto.CustomerDtos.KycSubmissionDto;
import com.banking.customer.repository.CustomerKycRepository;
import com.banking.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerKycService {

    private static final Logger log = LoggerFactory.getLogger(CustomerKycService.class);

    private final CustomerKycRepository kycRepository;
    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CustomerKycService(CustomerKycRepository kycRepository,
                              CustomerRepository customerRepository,
                              KafkaTemplate<String, Object> kafkaTemplate) {
        this.kycRepository = kycRepository;
        this.customerRepository = customerRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public CustomerKyc submitKyc(String customerId, KycSubmissionDto dto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        CustomerKyc.IdType idType = CustomerKyc.IdType.valueOf(dto.idType().toUpperCase());
        CustomerKyc kyc = new CustomerKyc(
                UUID.randomUUID().toString(),
                customer.getId(),
                idType,
                dto.idNumber(),
                dto.documentUrl()
        );

        CustomerKyc saved = kycRepository.save(kyc);
        log.info("KYC document [{}] submitted for customer [{}]", dto.idType(), customerId);

        kafkaTemplate.send("banking.customer.kyc.submitted", customerId, saved);
        return saved;
    }

    @Transactional
    public CustomerKyc reviewKyc(String kycId, KycReviewDto review) {
        CustomerKyc kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC Record", kycId));

        Customer customer = customerRepository.findById(kyc.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", kyc.getCustomerId()));

        if ("APPROVE".equalsIgnoreCase(review.action())) {
            kyc.approve(review.officerId());
            customer.activate(); // Fully activates banking capabilities
            customerRepository.save(customer);
            log.info("KYC approved for customer [{}]. Account status set to ACTIVE.", customer.getId());
            kafkaTemplate.send("banking.customer.kyc.approved", customer.getId(), kyc);
        } else {
            kyc.reject(review.rejectionReason(), review.officerId());
            log.warn("KYC rejected for customer [{}]. Reason: {}", customer.getId(), review.rejectionReason());
            kafkaTemplate.send("banking.customer.kyc.rejected", customer.getId(), kyc);
        }

        return kycRepository.save(kyc);
    }
}
