package com.banking.reporting.export.strategies;

import com.banking.common.exception.BankingException;
import com.banking.reporting.dto.ReportingDtos.StatementResponseDto;
import com.banking.reporting.dto.ReportingDtos.TransactionRecordDto;
import com.banking.reporting.export.ExportFormat;
import com.banking.reporting.export.TransactionExportStrategy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class ExcelExportStrategy implements TransactionExportStrategy {

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.EXCEL;
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

    @Override
    public byte[] export(StatementResponseDto data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Account Statement");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Currency Style
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat df = workbook.createDataFormat();
            currencyStyle.setDataFormat(df.getFormat("$#,##0.00"));

            // 1. Account Metadata Rows
            Row meta1 = sheet.createRow(0);
            meta1.createCell(0).setCellValue("Account Number:");
            meta1.createCell(1).setCellValue(data.accountNumber());
            meta1.createCell(3).setCellValue("Statement Period:");
            meta1.createCell(4).setCellValue(data.fromDate() + " to " + data.toDate());

            Row meta2 = sheet.createRow(1);
            meta2.createCell(0).setCellValue("Total Credits:");
            Cell crCell = meta2.createCell(1);
            crCell.setCellValue(data.summary().totalCredits().doubleValue());
            crCell.setCellStyle(currencyStyle);

            meta2.createCell(3).setCellValue("Total Debits:");
            Cell drCell = meta2.createCell(4);
            drCell.setCellValue(data.summary().totalDebits().doubleValue());
            drCell.setCellStyle(currencyStyle);

            // 2. Table Column Headers
            String[] columns = {"Transaction ID", "Date", "Type", "Amount", "Currency", "Channel", "Status"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3. Populate Rows
            int rowIdx = 4;
            for (TransactionRecordDto tx : data.transactions()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.transactionId());
                row.createCell(1).setCellValue(tx.timestamp().toString());
                row.createCell(2).setCellValue(tx.type());

                Cell amtCell = row.createCell(3);
                amtCell.setCellValue(tx.amount().doubleValue());
                amtCell.setCellStyle(currencyStyle);

                row.createCell(4).setCellValue(tx.currency());
                row.createCell(5).setCellValue(tx.channel());
                row.createCell(6).setCellValue(tx.status());
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BankingException("EXCEL_EXPORT_ERROR", "Failed to generate Excel statement", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
