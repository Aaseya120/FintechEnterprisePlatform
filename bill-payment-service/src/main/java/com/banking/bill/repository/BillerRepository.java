package com.banking.bill.repository;

import com.banking.bill.domain.Biller;
import com.banking.bill.domain.BillerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillerRepository extends JpaRepository<Biller, String> {

    Optional<Biller> findByBillerCodeAndActiveTrue(String billerCode);

    List<Biller> findByActiveTrue();

    List<Biller> findByCategoryAndActiveTrue(BillerCategory category);
}
