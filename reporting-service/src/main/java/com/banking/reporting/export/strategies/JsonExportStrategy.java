package com.banking.reporting.export.strategies;

import com.banking.common.exception.BankingException;
import com.banking.reporting.dto.ReportingDtos.StatementResponseDto;
import com.banking.reporting.export.ExportFormat;
import com.banking.reporting.export.TransactionExportStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JsonExportStrategy implements TransactionExportStrategy {

    private final ObjectMapper objectMapper;

    public JsonExportStrategy() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.JSON;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public byte[] export(StatementResponseDto data) {
        try {
            return objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BankingException("JSON_EXPORT_ERROR", "Failed to serialize statement to JSON", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
