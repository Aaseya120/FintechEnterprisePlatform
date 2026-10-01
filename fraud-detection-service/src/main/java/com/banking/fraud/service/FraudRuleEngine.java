package com.banking.fraud.service;

import com.banking.fraud.domain.FraudAlert;
import com.banking.fraud.dto.FraudDtos.FraudCheckRequestDto;
import com.banking.fraud.dto.FraudDtos.FraudCheckResultDto;
import com.banking.fraud.repository.FraudAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FraudRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudRuleEngine.class);

    private final StringRedisTemplate redisTemplate;
    private final FraudAlertRepository fraudAlertRepository;

    public FraudRuleEngine(StringRedisTemplate redisTemplate, FraudAlertRepository fraudAlertRepository) {
        this.redisTemplate = redisTemplate;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional
    public FraudCheckResultDto evaluateTransaction(FraudCheckRequestDto req) {
        int riskScore = 0;
        List<String> triggeredRules = new ArrayList<>();

        // Rule 1: High Velocity Sliding Window Check via Redis Sorted Set
        String velocityKey = "fraud:velocity:" + req.accountNumber();
        long now = System.currentTimeMillis();
        long windowStart = now - 60000; // 60-second window

        // Remove entries older than 60s
        redisTemplate.opsForZSet().removeRangeByScore(velocityKey, 0, windowStart);
        // Add current txn timestamp
        redisTemplate.opsForZSet().add(velocityKey, req.transactionId(), now);
        redisTemplate.expire(velocityKey, Duration.ofMinutes(5));

        Long txnCount = redisTemplate.opsForZSet().zCard(velocityKey);
        if (txnCount != null && txnCount > 3) {
            riskScore += 45;
            triggeredRules.add("HIGH_VELOCITY_BURST: " + txnCount + " transactions within 60 seconds");
        }

        // Rule 2: Outlier / High-Value Amount Check
        if (req.amount().compareTo(new BigDecimal("100000.00")) > 0) {
            riskScore += 50;
            triggeredRules.add("EXCESSIVE_AMOUNT_THRESHOLD: Transfer exceeds 100,000 threshold");
        } else if (req.amount().compareTo(new BigDecimal("50000.00")) > 0) {
            riskScore += 25;
            triggeredRules.add("HIGH_VALUE_TRANSACTION: Transfer exceeds 50,000 threshold");
        }

        // Rule 3: Geolocation Anomaly / Impossible Travel Check
        if (req.location() != null && !req.location().isBlank()) {
            String lastLocationKey = "fraud:last_loc:" + req.accountNumber();
            String prevLocation = redisTemplate.opsForValue().get(lastLocationKey);
            if (prevLocation != null && !prevLocation.equalsIgnoreCase(req.location())) {
                riskScore += 35;
                triggeredRules.add("IMPOSSIBLE_TRAVEL_ANOMALY: Location jumped from " + prevLocation + " to " + req.location());
            }
            redisTemplate.opsForValue().set(lastLocationKey, req.location(), Duration.ofHours(2));
        }

        // Decision calculation
        FraudAlert.RiskDecision decision;
        String recommendation;
        if (riskScore >= 75) {
            decision = FraudAlert.RiskDecision.REJECTED;
            recommendation = "Block transaction immediately; notify fraud investigation team";
        } else if (riskScore >= 40) {
            decision = FraudAlert.RiskDecision.REVIEW_REQUIRED;
            recommendation = "Challenge client with Out-Of-Band 2FA OTP / Biometric verification";
        } else {
            decision = FraudAlert.RiskDecision.APPROVED;
            recommendation = "Low risk transaction; proceed with clearing";
        }

        // Persist Alert if high/medium risk
        if (decision != FraudAlert.RiskDecision.APPROVED) {
            FraudAlert alert = new FraudAlert(
                    UUID.randomUUID().toString(),
                    req.transactionId(),
                    req.accountNumber(),
                    req.amount(),
                    riskScore,
                    decision,
                    String.join("; ", triggeredRules),
                    req.clientIp(),
                    req.location()
            );
            fraudAlertRepository.save(alert);
            log.warn("FRAUD RISK DETECTED: Decision [{}] Score [{}] Rules: {}", decision, riskScore, triggeredRules);
        }

        return new FraudCheckResultDto(
                req.transactionId(),
                req.accountNumber(),
                riskScore,
                decision,
                triggeredRules,
                recommendation,
                Instant.now()
        );
    }
}
