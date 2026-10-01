package com.banking.common.idempotency;

import com.banking.common.exception.BankingException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private final IdempotencyService idempotencyService;

    public IdempotencyAspect(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader(idempotent.headerName());

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BankingException("MISSING_IDEMPOTENCY_KEY",
                    String.format("Financial transaction requires mandatory header: %s", idempotent.headerName()),
                    HttpStatus.BAD_REQUEST);
        }

        // Hash the first payload argument
        Object[] args = joinPoint.getArgs();
        Object payload = (args.length > 0) ? args[0] : null;
        String requestHash = idempotencyService.computePayloadHash(payload);

        // Check if already completed
        Optional<IdempotentRecord> existingRecord = idempotencyService.getRecord(idempotencyKey);
        if (existingRecord.isPresent()) {
            IdempotentRecord record = existingRecord.get();
            if (record.status() == IdempotentRecord.IdempotencyStatus.COMPLETED) {
                log.info("Idempotent request recognized for key: '{}'. Returning cached response.", idempotencyKey);
                return ResponseEntity.status(record.httpStatusCode()).body(record.responseBody());
            }
        }

        // Acquire lock
        boolean locked = idempotencyService.lockOrRetrieve(idempotencyKey, requestHash, idempotent.lockTimeoutSeconds());
        if (!locked) {
            // Already handled by lockOrRetrieve throwing exception or returning cached
            Optional<IdempotentRecord> cached = idempotencyService.getRecord(idempotencyKey);
            if (cached.isPresent() && cached.get().status() == IdempotentRecord.IdempotencyStatus.COMPLETED) {
                return ResponseEntity.status(cached.get().httpStatusCode()).body(cached.get().responseBody());
            }
        }

        try {
            Object result = joinPoint.proceed();

            int statusCode = HttpStatus.OK.value();
            Object body = result;
            if (result instanceof ResponseEntity<?> responseEntity) {
                statusCode = responseEntity.getStatusCode().value();
                body = responseEntity.getBody();
            }

            idempotencyService.complete(idempotencyKey, body, statusCode, idempotent.ttl(), idempotent.timeUnit());
            return result;
        } catch (Throwable t) {
            log.error("Execution failed for idempotent key '{}'. Releasing in-flight lock.", idempotencyKey);
            idempotencyService.releaseLock(idempotencyKey);
            throw t;
        }
    }
}
