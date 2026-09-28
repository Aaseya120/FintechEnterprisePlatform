package com.enterprise.fintech.order.repository;

import com.enterprise.fintech.order.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.retryCount < 5 ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
}
