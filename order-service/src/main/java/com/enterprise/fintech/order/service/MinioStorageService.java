package com.enterprise.fintech.order.service;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:fintech-invoices}")
    private String bucketName;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void initializeBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception ex) {
            log.warn("MinIO bucket initialization notice: {}. Storage will attempt on-demand.", ex.getMessage());
        }
    }

    public String uploadOrderReceipt(String orderReference, String customerId, String content) {
        String objectName = "receipts/" + orderReference + ".txt";
        try {
            initializeBucket();
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(bais, bytes.length, -1)
                                .contentType("text/plain")
                                .build()
                );
            }

            // Generate presigned download URL valid for 24 hours
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(24, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception ex) {
            log.error("Failed to upload receipt to MinIO for order {}: {}", orderReference, ex.getMessage());
            // Graceful fallback for environments where MinIO is temporarily offline
            return "minio://" + bucketName + "/" + objectName;
        }
    }
}
