variable "aws_region" {
  description = "AWS Primary Deployment Region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "cluster_name" {
  description = "AWS EKS Cluster Name"
  type        = string
  default     = "banking-eks-prod"
}

variable "vpc_cidr" {
  description = "CIDR block for Banking Platform VPC"
  type        = string
  default     = "10.100.0.0/16"
}
