package com.banking.exchange.repository;

import com.banking.exchange.domain.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<CountryEntity, String> {

    List<CountryEntity> findAllByOrderByCountryNameAsc();

    Optional<CountryEntity> findByCountryCodeIgnoreCase(String countryCode);

    Optional<CountryEntity> findByAlpha3CodeIgnoreCase(String alpha3Code);
}
