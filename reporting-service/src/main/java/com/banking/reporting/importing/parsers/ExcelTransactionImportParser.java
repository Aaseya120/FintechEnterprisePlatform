package com.banking.reporting.importing.parsers;

import com.banking.common.exception.BankingException;
import com.banking.reporting.importing.ImportModels.ImportedTransactionDto;
import com.banking.reporting.importing.ImportModels.TransactionImportParser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelTransactionImportParser implements TransactionImportParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && (fileName.endsWith(".xlsx") || fileName.endsWith(".xls"));
    }

    @Override
    public List<ImportedTransactionDto> parse(InputStream inputStream) {
        List<ImportedTransactionDto> results = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row index 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String extRef = getCellString(row.getCell(0));
                String src = getCellString(row.getCell(1));
                String tgt = getCellString(row.getCell(2));
                double amt = row.getCell(3) != null ? row.getCell(3).getNumericCellValue() : 0.0;
                String curr = getCellString(row.getCell(4));
                String channel = getCellString(row.getCell(5));

                if (extRef.isBlank() || src.isBlank()) continue;

                results.add(new ImportedTransactionDto(
                        extRef, src, tgt, BigDecimal.valueOf(amt), curr.toUpperCase(), channel, LocalDate.now()
                ));
            }
        } catch (Exception e) {
            throw new BankingException("EXCEL_IMPORT_ERROR", "Failed to parse Excel transaction file", HttpStatus.BAD_REQUEST, e);
        }
        return results;
    }

    private final DataFormatter dataFormatter = new DataFormatter();

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return dataFormatter.formatCellValue(cell).trim();
    }
}
