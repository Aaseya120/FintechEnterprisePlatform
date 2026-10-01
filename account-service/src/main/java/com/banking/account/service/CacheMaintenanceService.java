package com.banking.account.service;

import com.banking.account.config.RedisCacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled Cache Maintenance Service:
 * 1. Periodic full cache invalidation to prevent stale financial data accumulation.
 * 2. Proactive balance cache refresh to ensure near-real-time accuracy for downstream consumers.
 *
 * Banking compliance note: Account balance caches must never exceed a staleness window
 * that could cause incorrect authorization decisions for debit/credit operations.
 */
@Service
public class CacheMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(CacheMaintenanceService.class);

    private final CacheManager cacheManager;

    public CacheMaintenanceService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Full cache invalidation every 15 minutes.
     * Ensures no stale account metadata persists beyond the maximum staleness window,
     * even if individual @CacheEvict annotations missed an edge case (e.g., direct DB updates,
     * batch jobs, or inter-service account modifications via Kafka events).
     */
    @Scheduled(fixedRate = 900_000, initialDelay = 900_000) // 15 minutes
    public void evictAllAccountCachesPeriodically() {
        evictCache(RedisCacheConfig.CACHE_ACCOUNTS);
        evictCache(RedisCacheConfig.CACHE_CUSTOMER_ACCOUNTS);
        log.info("Cache maintenance: Periodic full eviction completed for account and customer-accounts caches");
    }

    /**
     * Balance cache aggressive cleanup every 60 seconds.
     * Balance data is the most latency-sensitive — a stale balance can cause
     * double-spend or insufficient-funds false positives. This ensures the
     * maximum staleness never exceeds ~90 seconds (30s TTL + 60s sweep).
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 60_000) // 1 minute
    public void evictBalanceCacheAggressively() {
        evictCache(RedisCacheConfig.CACHE_ACCOUNT_BALANCES);
        log.debug("Cache maintenance: Balance cache sweep completed");
    }

    /**
     * Daily midnight full cache flush.
     * Banking day-end: clear all cached data to start the next business day fresh.
     * Aligns with end-of-day (EOD) batch reconciliation and interest posting cycles.
     */
    @Scheduled(cron = "0 0 0 * * *") // Midnight
    public void dailyFullCacheFlush() {
        evictCache(RedisCacheConfig.CACHE_ACCOUNTS);
        evictCache(RedisCacheConfig.CACHE_ACCOUNT_BALANCES);
        evictCache(RedisCacheConfig.CACHE_CUSTOMER_ACCOUNTS);
        log.info("Cache maintenance: Daily midnight full cache flush completed (EOD cycle)");
    }

    private void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
