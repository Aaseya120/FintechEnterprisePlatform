package com.banking.exchange.repository;

import com.banking.exchange.domain.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, String> {

    List<CurrencyEntity> findByActiveTrueOrderByCodeAsc();
}
