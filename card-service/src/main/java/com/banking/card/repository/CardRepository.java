package com.banking.card.repository;

import com.banking.card.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByCustomerId(String customerId);
    Optional<Card> findByCardNumber(String cardNumber);
    List<Card> findByLinkedAccountNumber(String linkedAccountNumber);
}
