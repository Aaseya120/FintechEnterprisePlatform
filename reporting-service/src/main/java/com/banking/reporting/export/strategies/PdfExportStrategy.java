package com.banking.reporting.export.strategies;

import com.banking.common.exception.BankingException;
import com.banking.reporting.dto.ReportingDtos.StatementResponseDto;
import com.banking.reporting.dto.ReportingDtos.TransactionRecordDto;
import com.banking.reporting.export.ExportFormat;
import com.banking.reporting.export.TransactionExportStrategy;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Component
public class PdfExportStrategy implements TransactionExportStrategy {

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PDF;
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }

    @Override
    public byte[] export(StatementResponseDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header Banner
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(0, 51, 102));
            Paragraph title = new Paragraph("CORE BANKING ACCOUNT STATEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);

            // 2. Account Metadata Box
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);

            metaTable.addCell(createCell("Account Number: " + data.accountNumber(), metaFont, false));
            metaTable.addCell(createCell("Statement Period: " + data.fromDate() + " to " + data.toDate(), metaFont, false));
            metaTable.addCell(createCell("Total Credits: $" + data.summary().totalCredits(), metaFont, false));
            metaTable.addCell(createCell("Total Debits: $" + data.summary().totalDebits(), metaFont, false));
            metaTable.addCell(createCell("Net Cash Flow: $" + data.summary().netCashFlow(), metaFont, false));
            metaTable.addCell(createCell("Total Transactions: " + data.summary().totalTransactions(), metaFont, false));
            document.add(metaTable);

            // 3. Transactions Table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 2.5f, 1.5f, 2.0f, 1.5f, 1.8f, 2.0f});

            // Table Headers
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            String[] headers = {"Txn ID", "Date", "Type", "Amount", "Currency", "Channel", "Status"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setBackgroundColor(new Color(0, 51, 102));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Table Data Rows
            Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            boolean alternate = false;
            for (TransactionRecordDto tx : data.transactions()) {
                Color bg = alternate ? new Color(245, 245, 245) : Color.WHITE;

                table.addCell(createRowCell(tx.transactionId(), rowFont, bg, Element.ALIGN_LEFT));
                table.addCell(createRowCell(tx.timestamp().toString().substring(0, 19), rowFont, bg, Element.ALIGN_CENTER));
                table.addCell(createRowCell(tx.type(), rowFont, bg, Element.ALIGN_CENTER));

                // Color amounts: Green for credit, Red for debit
                Font amtFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                        "CREDIT".equalsIgnoreCase(tx.type()) ? new Color(0, 128, 0) : new Color(178, 34, 34));
                table.addCell(createRowCell(tx.amount().toString(), amtFont, bg, Element.ALIGN_RIGHT));

                table.addCell(createRowCell(tx.currency(), rowFont, bg, Element.ALIGN_CENTER));
                table.addCell(createRowCell(tx.channel(), rowFont, bg, Element.ALIGN_CENTER));
                table.addCell(createRowCell(tx.status(), rowFont, bg, Element.ALIGN_CENTER));

                alternate = !alternate;
            }
            document.add(table);

            // 4. Footer Note
            Paragraph footer = new Paragraph("\nThis document is a certified computer-generated account statement issued under regulatory compliance standards.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BankingException("PDF_EXPORT_ERROR", "Failed to generate PDF account statement", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private PdfPCell createCell(String text, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (!border) cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell createRowCell(String text, Font font, Color bg, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }
}
