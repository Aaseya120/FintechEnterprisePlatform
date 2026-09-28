package com.enterprise.fintech.order.repository;

import com.enterprise.fintech.order.domain.Order;
import com.enterprise.fintech.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderReference(String orderReference);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(OrderStatus status);
}
