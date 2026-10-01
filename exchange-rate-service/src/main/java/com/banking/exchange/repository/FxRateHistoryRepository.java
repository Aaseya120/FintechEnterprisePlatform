package com.banking.exchange.repository;

import com.banking.exchange.domain.FxRateHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FxRateHistoryRepository extends JpaRepository<FxRateHistoryEntity, String> {

    @Query("SELECT h FROM FxRateHistoryEntity h WHERE UPPER(h.fromCurrency) = UPPER(:from) AND UPPER(h.toCurrency) = UPPER(:to) ORDER BY h.recordedAt DESC")
    List<FxRateHistoryEntity> findLatestTicks(@Param("from") String from, @Param("to") String to, Pageable pageable);
}
