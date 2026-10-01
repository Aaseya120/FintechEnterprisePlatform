package com.banking.common.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * Header name containing the idempotency key.
     */
    String headerName() default "Idempotency-Key";

    /**
     * Time-to-live for the idempotency record. Default is 24 hours.
     */
    long ttl() default 24;

    /**
     * Time unit for the TTL.
     */
    TimeUnit timeUnit() default TimeUnit.HOURS;

    /**
     * Timeout for holding the in-flight processing lock.
     */
    long lockTimeoutSeconds() default 60;
}
