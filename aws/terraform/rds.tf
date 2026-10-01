# AWS Relational Database Service: Aurora PostgreSQL Multi-AZ & Oracle 19c Enterprise

resource "aws_db_subnet_group" "db_subnet_group" {
  name       = "banking-db-subnet-group"
  subnet_ids = aws_subnet.database[*].id

  tags = {
    Name = "banking-db-subnet-group"
  }
}

resource "aws_security_group" "rds_sg" {
  name        = "banking-rds-security-group"
  description = "Allow inbound database traffic from EKS worker nodes"
  vpc_id      = aws_vpc.banking_vpc.id

  ingress {
    description = "PostgreSQL from Private Subnets"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = aws_subnet.private[*].cidr_block
  }

  ingress {
    description = "Oracle 19c TNS from Private Subnets"
    from_port   = 1521
    to_port     = 1521
    protocol    = "tcp"
    cidr_blocks = aws_subnet.private[*].cidr_block
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 1. AWS Aurora PostgreSQL Multi-AZ Cluster (Primary + Read Replica)
resource "aws_rds_cluster" "postgresql_cluster" {
  cluster_identifier      = "banking-postgres-cluster"
  engine                  = "aurora-postgresql"
  engine_version          = "16.1"
  database_name           = "banking_db"
  master_username         = "banking_admin"
  master_password         = "SecureProdBanking2026!#"
  db_subnet_group_name    = aws_db_subnet_group.db_subnet_group.name
  vpc_security_group_ids  = [aws_security_group.rds_sg.id]
  storage_encrypted       = true
  deletion_protection     = true
  backup_retention_period = 35
  preferred_backup_window = "02:00-03:00"
}

resource "aws_rds_cluster_instance" "cluster_instances" {
  count              = 2
  identifier         = "banking-postgres-instance-${count.index + 1}"
  cluster_identifier = aws_rds_cluster.postgresql_cluster.id
  instance_class     = "db.r6g.xlarge"
  engine             = aws_rds_cluster.postgresql_cluster.engine
  engine_version     = aws_rds_cluster.postgresql_cluster.engine_version
}

# 2. Oracle 19c Enterprise Edition RDS Instance (for CBS batch & stored procedures)
resource "aws_db_instance" "oracle_cbs" {
  identifier              = "banking-oracle-19c-cbs"
  engine                  = "oracle-ee"
  engine_version          = "19.0.0.0.ru-2024-04.rur-2024-04.r1"
  instance_class          = "db.m6i.2xlarge"
  allocated_storage       = 500
  max_allocated_storage   = 2000
  storage_type            = "gp3"
  username                = "cbs_admin"
  password                = "OracleCbsEnterprise2026!#"
  db_subnet_group_name    = aws_db_subnet_group.db_subnet_group.name
  vpc_security_group_ids  = [aws_security_group.rds_sg.id]
  multi_az                = true
  storage_encrypted       = true
  backup_retention_period = 30
  skip_final_snapshot     = false
}
