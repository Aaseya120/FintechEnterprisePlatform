package com.banking.exchange.repository;

import com.banking.exchange.domain.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, String> {

    Optional<ExchangeRateEntity> findByFromCurrencyIgnoreCaseAndToCurrencyIgnoreCase(String fromCurrency, String toCurrency);

    List<ExchangeRateEntity> findByFromCurrencyIgnoreCase(String fromCurrency);

    List<ExchangeRateEntity> findAllByOrderByFromCurrencyAscToCurrencyAsc();
}
