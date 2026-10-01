# AWS S3 KYC Document Vault & AWS KMS Customer Managed Key

resource "aws_kms_key" "banking_kms" {
  description             = "KMS Master Key for Core Banking S3 KYC and Database Encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true

  tags = {
    Name = "banking-kms-master-key"
  }
}

resource "aws_s3_bucket" "kyc_vault" {
  bucket        = "banking-kyc-vault-${var.environment}"
  force_destroy = false
}

resource "aws_s3_bucket_server_side_encryption_configuration" "kyc_encryption" {
  bucket = aws_s3_bucket.kyc_vault.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.banking_kms.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "kyc_access_block" {
  bucket                  = aws_s3_bucket.kyc_vault.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
