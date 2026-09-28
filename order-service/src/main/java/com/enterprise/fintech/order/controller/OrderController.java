package com.enterprise.fintech.order.controller;

import com.enterprise.fintech.order.dto.CreateOrderRequest;
import com.enterprise.fintech.order.dto.OrderResponse;
import com.enterprise.fintech.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderReference}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderReference) {
        return ResponseEntity.ok(orderService.getOrderByReference(orderReference));
    }
}
