# AWS ElastiCache Redis Cluster: Distributed Caching & Token Bucket Rate Limiting

resource "aws_elasticache_subnet_group" "redis_subnet_group" {
  name       = "banking-redis-subnet-group"
  subnet_ids = aws_subnet.database[*].id
}

resource "aws_security_group" "redis_sg" {
  name        = "banking-redis-security-group"
  description = "Allow inbound Redis traffic from EKS worker nodes"
  vpc_id      = aws_vpc.banking_vpc.id

  ingress {
    from_port   = 6379
    to_port     = 6379
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

resource "aws_elasticache_replication_group" "redis_cluster" {
  replication_group_id       = "banking-redis-cluster"
  description                = "Multi-AZ Redis Cluster for Rate Limiting, Idempotency and Session Caching"
  engine                     = "redis"
  engine_version             = "7.1"
  node_type                  = "cache.r6g.xlarge"
  num_cache_clusters         = 3
  parameter_group_name       = "default.redis7.cluster.on"
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.redis_subnet_group.name
  security_group_ids         = [aws_security_group.redis_sg.id]
  automatic_failover_enabled = true
  multi_az_enabled           = true
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
}
