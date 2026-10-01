package com.banking.reporting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "report_audit_logs",
    indexes = {
        @Index(name = "idx_report_account_date", columnList = "account_number, exported_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ReportAuditLog {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "account_number", length = 34, nullable = false)
    private String accountNumber;

    @Column(name = "export_format", length = 10, nullable = false)
    private String exportFormat;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "requested_by", length = 50)
    private String requestedBy;

    @Column(name = "exported_at", nullable = false)
    private Instant exportedAt;

    public ReportAuditLog(String id, String accountNumber, String exportFormat,
                          Integer recordCount, Long fileSizeBytes, String fileName,
                          String requestedBy, Instant exportedAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.exportFormat = exportFormat;
        this.recordCount = recordCount;
        this.fileSizeBytes = fileSizeBytes;
        this.fileName = fileName;
        this.requestedBy = requestedBy;
        this.exportedAt = exportedAt;
    }
}
