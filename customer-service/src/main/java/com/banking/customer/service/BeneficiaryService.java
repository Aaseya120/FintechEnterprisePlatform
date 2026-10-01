package com.banking.customer.service;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.customer.domain.Beneficiary;
import com.banking.customer.dto.CustomerDtos.BeneficiaryRequestDto;
import com.banking.customer.dto.CustomerDtos.BeneficiaryResponseDto;
import com.banking.customer.repository.BeneficiaryRepository;
import com.banking.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BeneficiaryService {

    private static final Logger log = LoggerFactory.getLogger(BeneficiaryService.class);
    private static final BigDecimal COOLING_PERIOD_LIMIT = new BigDecimal("50000.00"); // Standard bank 4-hour cooling limit

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository, CustomerRepository customerRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BeneficiaryResponseDto addBeneficiary(String customerId, BeneficiaryRequestDto req) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", customerId);
        }

        Beneficiary.BeneficiaryType type = Beneficiary.BeneficiaryType.valueOf(req.beneficiaryType().toUpperCase());
        Beneficiary beneficiary = new Beneficiary(
                UUID.randomUUID().toString(),
                customerId,
                req.beneficiaryName(),
                req.accountNumber(),
                req.bankName(),
                req.routingOrIfscCode(),
                type,
                req.maxTransferLimit(),
                4 // 4-hour cooling-off period
        );

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary [{}] added for customer [{}] with cooling-off period ending at {}",
                saved.getBeneficiaryName(), customerId, saved.getCoolingEndTime());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponseDto> listBeneficiaries(String customerId) {
        return beneficiaryRepository.findByCustomerIdAndIsActiveTrue(customerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void validateTransferAllowance(String beneficiaryId, String customerId, BigDecimal amount) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(beneficiaryId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", beneficiaryId));

        if (!beneficiary.isActive()) {
            throw new BankingException("BENEFICIARY_INACTIVE", "Beneficiary is inactive", HttpStatus.BAD_REQUEST);
        }

        if (beneficiary.isInCoolingPeriod() && amount.compareTo(COOLING_PERIOD_LIMIT) > 0) {
            throw new BankingException("COOLING_PERIOD_LIMIT_EXCEEDED",
                    String.format("Transfer of %s exceeds cooling-off allowance of %s. Cooling ends at %s",
                            amount, COOLING_PERIOD_LIMIT, beneficiary.getCoolingEndTime()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (amount.compareTo(beneficiary.getMaxTransferLimit()) > 0) {
            throw new BankingException("BENEFICIARY_LIMIT_EXCEEDED",
                    String.format("Transfer exceeds configured limit of %s for this beneficiary", beneficiary.getMaxTransferLimit()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private BeneficiaryResponseDto mapToDto(Beneficiary b) {
        return new BeneficiaryResponseDto(
                b.getId(),
                b.getCustomerId(),
                b.getBeneficiaryName(),
                b.getAccountNumber(),
                b.getBankName(),
                b.getRoutingOrIfscCode(),
                b.getBeneficiaryType().name(),
                b.getMaxTransferLimit(),
                b.isInCoolingPeriod(),
                b.getCoolingEndTime(),
                b.isActive()
        );
    }
}
