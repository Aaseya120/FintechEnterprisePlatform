package com.banking.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static AesGcmCryptoService cryptoService;

    @Autowired
    public void setCryptoService(AesGcmCryptoService service) {
        EncryptedStringConverter.cryptoService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        if (cryptoService == null) {
            throw new IllegalStateException(
                    "AesGcmCryptoService has not been initialized by Spring DI. " +
                    "Ensure the converter is used within a Spring-managed context.");
        }
        return cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (cryptoService == null) {
            throw new IllegalStateException(
                    "AesGcmCryptoService has not been initialized by Spring DI. " +
                    "Ensure the converter is used within a Spring-managed context.");
        }
        return cryptoService.decrypt(dbData);
    }
}
