package com.banking.exchange.service;

import com.banking.exchange.domain.ExchangeRateEntity;
import com.banking.exchange.domain.FxRateHistoryEntity;
import com.banking.exchange.repository.ExchangeRateRepository;
import com.banking.exchange.repository.FxRateHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Industry-standard Dynamic FX Rate Fluctuation Engine:
 * Simulates interbank Brownian motion ticker ticks, recalculating Bid/Ask spreads
 * and invalidating Redis caches to reflect live foreign exchange market movements.
 */
@Service
public class DynamicForexRateEngine {

    private static final Logger log = LoggerFactory.getLogger(DynamicForexRateEngine.class);

    private final ExchangeRateRepository rateRepository;
    private final FxRateHistoryRepository historyRepository;
    private final CacheManager cacheManager;

    public DynamicForexRateEngine(ExchangeRateRepository rateRepository,
                                  FxRateHistoryRepository historyRepository,
                                  CacheManager cacheManager) {
        this.rateRepository = rateRepository;
        this.historyRepository = historyRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Scheduled market tick every 30 seconds (simulating continuous Reuters/Bloomberg FX feeds).
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    @Transactional
    public void scheduledMarketTick() {
        fluctuateAllRates("INTERBANK_FEED");
    }

    /**
     * Programmatic trigger for dynamic rate changes on demand.
     */
    @Transactional
    public List<ExchangeRateEntity> fluctuateAllRates(String tickType) {
        var rates = rateRepository.findAll();
        if (rates.isEmpty()) {
            return List.of();
        }

        var historyList = new ArrayList<FxRateHistoryEntity>();

        for (var rate : rates) {
            // Pegged currency handling (e.g. AED and SAR pegged to USD)
            boolean isPegged = (rate.getFromCurrency().equals("USD") && (rate.getToCurrency().equals("AED") || rate.getToCurrency().equals("SAR")))
                    || ((rate.getFromCurrency().equals("AED") || rate.getFromCurrency().equals("SAR")) && rate.getToCurrency().equals("USD"));

            if (isPegged) {
                continue; // Do not fluctuate pegged currencies
            }

            // Normal fluctuation delta between -0.15% and +0.15%
            double deltaPercent = (ThreadLocalRandom.current().nextDouble() * 0.30 - 0.15); // e.g. -0.05%
            BigDecimal deltaMultiplier = BigDecimal.valueOf(1.0 + (deltaPercent / 100.0));

            BigDecimal oldMid = rate.getMidRate();
            BigDecimal newMid = oldMid.multiply(deltaMultiplier).setScale(6, RoundingMode.HALF_UP);

            BigDecimal halfSpread = rate.getSpreadPercentage().divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            BigDecimal newBid = newMid.multiply(BigDecimal.ONE.subtract(halfSpread)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal newAsk = newMid.multiply(BigDecimal.ONE.add(halfSpread)).setScale(6, RoundingMode.HALF_UP);

            BigDecimal change24h = rate.getChange24hPercentage().add(BigDecimal.valueOf(deltaPercent)).setScale(2, RoundingMode.HALF_UP);

            rate.setMidRate(newMid);
            rate.setBidRate(newBid);
            rate.setAskRate(newAsk);
            rate.setChange24hPercentage(change24h);
            rate.setLastUpdatedAt(Instant.now());

            historyList.add(new FxRateHistoryEntity(
                    UUID.randomUUID().toString(),
                    rate.getFromCurrency(),
                    rate.getToCurrency(),
                    newMid,
                    tickType
            ));
        }

        rateRepository.saveAll(rates);
        historyRepository.saveAll(historyList);

        // Invalidate Redis cache to ensure fresh rates across all microservices
        var cache = cacheManager.getCache("exchangeRates");
        if (cache != null) {
            cache.clear();
        }

        log.info("Dynamic FX market tick completed. Updated {} currency pairs (Tick: {})", rates.size(), tickType);
        return rates;
    }
}
