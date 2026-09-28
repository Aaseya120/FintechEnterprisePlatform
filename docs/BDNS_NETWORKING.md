# Enterprise BDNS (Banking Domain Name System) & Service Discovery

## 1. What is BDNS in Enterprise Banking?

In mission-critical banking and fintech infrastructure, public DNS is insufficient for regulatory, security, and low-latency internal routing. **BDNS (Banking / Business Domain Name System)** refers to the private, high-security, split-horizon DNS architecture deployed within financial data centers and hybrid-cloud Kubernetes clusters.

BDNS fulfills three primary requirements:
1. **Zero-Trust Network Isolation**: Internal service hostnames (`*.fintech.enterprise.internal`) are never exposed to public root servers.
2. **Geo-Redundant Load Balancing**: Dynamic DNS routing between on-premise Oracle exadata clusters and AWS/Azure cloud pods based on real-time health probes.
3. **Mutual TLS (mTLS) Identity Verification**: BDNS names are cryptographically bound to X.509 certificates validated at the Ingress and Spring Cloud Gateway layers.

---

## 2. Platform Domain Hierarchy

| FQDN | Target Component | Resolution Strategy |
| :--- | :--- | :--- |
| `api.fintech.enterprise.internal` | Spring Cloud Gateway Ingress | External & B2B Partner Entry Point (mTLS + OAuth2) |
| `order-service.fintech-platform.svc.cluster.local` | Order Service Pods | Internal Kubernetes ClusterIP (Port 8081) |
| `payment-service.fintech-platform.svc.cluster.local` | Payment Service Pods | Internal gRPC ClusterIP (Port 9090) |
| `activemq.fintech.enterprise.internal` | ActiveMQ Artemis Cluster | JMS Core TCP (Port 61616) |
| `oracle-primary.fintech.enterprise.internal` | Oracle RAC Primary Node | JDBC Thin Client Connection (Port 1521) |
| `minio.fintech.enterprise.internal` | MinIO Distributed S3 Cluster | S3 REST API (Port 9000) |

---

## 3. Kubernetes CoreDNS Integration with BDNS

In production Kubernetes clusters, the `coredns` ConfigMap forwards queries for `.internal` enterprise zones to the upstream BDNS corporate DNS appliances:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns
  namespace: kube-system
data:
  Corefile: |
    .:53 {
        errors
        health {
           lameduck 5s
        }
        ready
        kubernetes cluster.local in-addr.arpa ip6.arpa {
           pods insecure
           fallthrough in-addr.arpa ip6.arpa
           ttl 30
        }
        prometheus :9153
        forward . /etc/resolv.conf {
           max_concurrent 1000
        }
        cache 30
        loop
        reload
        loadbalance
    }
    fintech.enterprise.internal:53 {
        forward . 10.200.1.10 10.200.1.11 {
            prefer_udp
        }
        cache 10
    }
```

---

## 4. Multi-Region Failover Architecture

```
                  [ Global Anycast BDNS ]
                             |
             +---------------+---------------+
             |                               |
       (Health: OK)                    (Health: OK)
             |                               |
   [ Data Center 1 - Primary ]     [ Data Center 2 - Secondary ]
   - Spring Cloud Gateway          - Spring Cloud Gateway
   - Order & Payment Pods          - Order & Payment Pods
   - ActiveMQ Primary Broker       - ActiveMQ Replica Broker
   - Oracle Data Guard Primary     - Oracle Data Guard Standby
```
When DC-1 experiences latency spikes or pod failures, BDNS automated health probes dynamically shift 100% of ingress traffic to DC-2 within sub-3-second TTL windows.
