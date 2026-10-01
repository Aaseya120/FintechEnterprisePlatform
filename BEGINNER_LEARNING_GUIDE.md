# 🎓 Enterprise Core Banking Platform: Fresher's Step-by-Step Learning Guide

Welcome to the **Enterprise Core Banking Microservices Ecosystem**! 

If you are a fresher or junior developer looking at this repository for the first time, an enterprise banking system with 11 microservices, Kafka, Redis, PostgreSQL, Oracle 19c PL/SQL, GraphQL, and SOAP can feel overwhelming.

Don't worry! This guide is designed specifically for you. It breaks down the entire project into a logical **"Follow the Money & Customer"** journey, explaining:
1. **Where to start first** and the recommended reading order.
2. **How real banking transactions work** behind the scenes.
3. **End-to-end sample flows with actual JSON requests and responses**.
4. **The industry design patterns** used in this project and why they exist.

---

## 🗺️ The Recommended 8-Step Learning Roadmap

Follow this exact sequence to build your mental model step-by-step:

```mermaid
flowchart LR
    Step1["1. Common Lib\n(banking-common)"] --> Step2["2. Customer & KYC\n(customer-service)"]
    Step2 --> Step3["3. Account & Ledger\n(account-service)"]
    Step3 --> Step4["4. Global FX Rates\n(exchange-rate-service)"]
    Step4 --> Step5["5. Payments & Saga\n(payment-service)"]
    Step5 --> Step6["6. Fraud & Risk\n(fraud-detection)"]
    Step6 --> Step7["7. Notifications\n(notification-service)"]
    Step7 --> Step8["8. Statements & Recon\n(reporting & batch)"]
```

| Step | Service | What You Learn Here | Time to Spend |
| :---: | :--- | :--- | :---: |
| **1** | [**banking-common**](file:///d:/Projects/Resume_Project/banking-common) | Base DTOs, AES-256-GCM encryption, PCI masking, Idempotency | 1 hour |
| **2** | [**customer-service**](file:///d:/Projects/Resume_Project/customer-service) | Customer onboarding, KYC documents, beneficiary cooling period | 1.5 hours |
| **3** | [**account-service**](file:///d:/Projects/Resume_Project/account-service) | Checking/Savings balance ledger, GraphQL API, Legacy CBS SOAP Bridge | 2 hours |
| **4** | [**exchange-rate-service**](file:///d:/Projects/Resume_Project/exchange-rate-service) | ISO currencies, country IBAN rules, real-time dynamic FX market ticker | 1 hour |
| **5** | [**payment-service**](file:///d:/Projects/Resume_Project/payment-service) | Multi-rail transfers (UPI, NEFT, IMPS, Cards, FX), 2-Phase Saga, Outbox | 3 hours |
| **6** | [**fraud-detection-service**](file:///d:/Projects/Resume_Project/fraud-detection-service) | Velocity rules, sliding-window Redis sorted sets, risk score engine | 1.5 hours |
| **7** | [**notification-service**](file:///d:/Projects/Resume_Project/notification-service) | Kafka event listeners, multi-channel dispatch (SMS, Email, Push) | 1 hour |
| **8** | [**reporting-service**](file:///d:/Projects/Resume_Project/reporting-service) & [**batch-service**](file:///d:/Projects/Resume_Project/batch-service) | PDF/Excel/CSV exports, Oracle 19c PL/SQL batching, automated reconciliation | 2 hours |

---

## 🚶 Step 1: Start with the Foundation (`banking-common`)

**Why start here?** Every microservice depends on this shared library. It contains no database tables, just clean reusable utilities and contracts.

### Key Files to Read:
1. [`DataMaskingUtil.java`](file:///d:/Projects/Resume_Project/banking-common/src/main/java/com/banking/common/crypto/DataMaskingUtil.java):
   - Shows PCI-DSS and GDPR compliant masking.
   - Example: A card number `4111111111111234` becomes `4111-XXXX-XXXX-1234`.
2. [`AesGcmCryptoService.java`](file:///d:/Projects/Resume_Project/banking-common/src/main/java/com/banking/common/crypto/AesGcmCryptoService.java):
   - Encrypts sensitive personally identifiable information (PII like Passports, SSNs, National IDs) using **AES-256-GCM** with a fresh 96-bit random Initialization Vector (IV).
3. [`EncryptedStringConverter.java`](file:///d:/Projects/Resume_Project/banking-common/src/main/java/com/banking/common/crypto/EncryptedStringConverter.java):
   - Transparent JPA converter: when an entity saves to DB, it encrypts; when it reads from DB, it decrypts.
4. [`IdempotencyAspect.java`](file:///d:/Projects/Resume_Project/banking-common/src/main/java/com/banking/common/idempotency/IdempotencyAspect.java):
   - Prevents double-charging if a user clicks "Pay" twice by caching the `Idempotency-Key` in Redis.

---

## 🚶 Step 2: Digital Customer Onboarding, eKYC & Video KYC (`customer-service`)

**Business Story:** In modern branchless digital banking, a customer registers online from their mobile phone. Regulators (FATF, US FinCEN, EU AML5, RBI V-KYC) mandate strict multi-factor verification before any financial account can be opened:

```mermaid
flowchart TD
    A["1. Demographics\n(Name, DOB, Email, Phone)"] --> B["2. Proof of Identity (POI)\n(Passport / National ID / PAN / SSN)"]
    B --> C["3. Proof of Address (POA)\n(Utility Bill / Bank Statement)"]
    C --> D["4. AI Biometric Liveness\n(Facial Scan & Anti-Spoofing Check)"]
    D --> E["5. Video & Voice KYC (V-KYC)\n(Session Video, Audio Sample, GPS Tag)"]
    E --> F["6. Compliance Review\n(Maker-Checker Approval / Rejection)"]
    F --> G["7. Customer ACTIVE\n(Ready for Account Opening)"]
```

### Key Pillars of Digital Onboarding in this Project:
1. **Proof of Identity (POI):** National ID, Passport, Driver's License, PAN, SSN, or Aadhaar. The ID number is encrypted via AES-256-GCM in the database (`customer_kyc.id_number`).
2. **Proof of Address (POA):** Utility bills, bank statements, or rental agreements uploaded with document type.
3. **Biometric Liveness & Anti-Spoofing Detection:** AI neural engine calculates a liveness confidence score (0.000 to 1.000) verifying a live human (not a printed photo, mask, or deepfake).
4. **Video KYC (V-KYC) & Audio Challenge:** WebRTC recorded video URL and spoken voice sample URL archived in secure S3 vault storage.
5. **GPS Geolocation Tagging:** Captures exact `geoLatitude` and `geoLongitude` at the moment of onboarding to prevent cross-border money mule registrations.
6. **Automated OCR Data Extraction:** Machine-readable zone (MRZ) and OCR text extracted from uploaded documents.
7. **Compliance Officer Review:** Endpoints to approve or reject with audit trail (`verifiedBy`, `verifiedAt`, `rejectionReason`).

### Key Files to Read:
1. [`Customer.java`](file:///d:/Projects/Resume_Project/customer-service/src/main/java/com/banking/customer/domain/Customer.java): The customer record with status (`ACTIVE`, `SUSPENDED`).
2. [`CustomerKyc.java`](file:///d:/Projects/Resume_Project/customer-service/src/main/java/com/banking/customer/domain/CustomerKyc.java): Contains V-KYC, POI/POA, AES-256-GCM encryption, liveness score, video/audio URLs, and GPS tags.
3. [`CustomerKycService.java`](file:///d:/Projects/Resume_Project/customer-service/src/main/java/com/banking/customer/service/CustomerKycService.java): Coordinates document upload, AI liveness scoring, and officer approval.
4. [`Beneficiary.java`](file:///d:/Projects/Resume_Project/customer-service/src/main/java/com/banking/customer/domain/Beneficiary.java): Contains the **Cooling-Off Period** (`coolingEndTime`).

---

### Sample Walkthrough 1A: Register Customer Demographics
```http
POST http://localhost:8080/api/v1/customers/onboard
Content-Type: application/json

{
  "firstName": "Alexander",
  "lastName": "Hamilton",
  "email": "a.hamilton@banking-domain.com",
  "phone": "+12125550190",
  "dateOfBirth": "1985-01-11",
  "address": "55 Wall St, New York, NY 10005, USA"
}
```
**Response:**
```json
{
  "success": true,
  "data": {
    "id": "CUST-001",
    "customerNumber": "CN-10001",
    "firstName": "Alexander",
    "lastName": "Hamilton",
    "email": "a.hamilton@banking-domain.com",
    "status": "ACTIVE"
  }
}
```

---

### Sample Walkthrough 1B: Submit Full Digital V-KYC (ID, POA, Video, Audio, GPS)
```http
POST http://localhost:8080/api/v1/customers/CUST-001/kyc
Content-Type: application/json

{
  "idType": "PASSPORT",
  "idNumber": "US984729104",
  "documentUrl": "https://s3.amazonaws.com/banking-kyc-vault/docs/passports/cust-001.pdf",
  "addressProofType": "UTILITY_BILL",
  "addressProofUrl": "https://s3.amazonaws.com/banking-kyc-vault/docs/utility/coned-2026.pdf",
  "selfieUrl": "https://s3.amazonaws.com/banking-kyc-vault/biometrics/selfies/cust-001-headshot.jpg",
  "videoKycUrl": "https://s3.amazonaws.com/banking-kyc-vault/vkyc/recordings/session_cust_001.mp4",
  "audioSampleUrl": "https://s3.amazonaws.com/banking-kyc-vault/vkyc/audio/voice_challenge_cust_001.wav",
  "geoLatitude": 40.706086,
  "geoLongitude": -74.008863,
  "ocrExtractedData": "{\"name\":\"Alexander Hamilton\",\"dob\":\"1985-01-11\",\"nationality\":\"USA\",\"passport_no\":\"US984729104\"}"
}
```
**Response:**
```json
{
  "success": true,
  "message": "KYC documents submitted for review",
  "data": {
    "id": "KYC-001",
    "customerId": "CUST-001",
    "idType": "PASSPORT",
    "idNumber": "ENC:AES-GCM:US984729104",
    "documentUrl": "https://s3.amazonaws.com/banking-kyc-vault/docs/passports/cust-001.pdf",
    "addressProofType": "UTILITY_BILL",
    "addressProofUrl": "https://s3.amazonaws.com/banking-kyc-vault/docs/utility/coned-2026.pdf",
    "selfieUrl": "https://s3.amazonaws.com/banking-kyc-vault/biometrics/selfies/cust-001-headshot.jpg",
    "livenessScore": 0.985,
    "livenessStatus": "PASSED",
    "videoKycUrl": "https://s3.amazonaws.com/banking-kyc-vault/vkyc/recordings/session_cust_001.mp4",
    "audioSampleUrl": "https://s3.amazonaws.com/banking-kyc-vault/vkyc/audio/voice_challenge_cust_001.wav",
    "geoLatitude": 40.706086,
    "geoLongitude": -74.008863,
    "verificationStatus": "SUBMITTED",
    "createdAt": "2026-10-01T22:50:00Z"
  }
}
```

---

### Sample Walkthrough 1C: Compliance Officer Review & Approval
```http
POST http://localhost:8080/api/v1/customers/kyc/KYC-001/review
Content-Type: application/json

{
  "action": "APPROVE",
  "rejectionReason": null,
  "officerId": "COMPLIANCE_OFFICER_01"
}
```
**Response:**
```json
{
  "success": true,
  "message": "KYC review processed",
  "data": {
    "id": "KYC-001",
    "verificationStatus": "APPROVED",
    "verifiedBy": "COMPLIANCE_OFFICER_01",
    "verifiedAt": "2026-10-01T22:52:00Z"
  }
}
```

---

### Sample Walkthrough 1D: End-to-End Onboarding Approval & Automatic Account/Credentials Generation
When the compliance backoffice approves a customer's submitted documents, the system automatically:
1. Activates customer profile (`ONBOARDING` -> `ACTIVE`).
2. Auto-provisions the primary core banking account number (`ACC...`).
3. Generates secure initial login credentials (username + 12-character high-entropy temporary password) with `must_change_password = true`.
4. Emits `banking.customer.onboarded` event to Kafka.

```http
POST http://localhost:8080/api/v1/customers/CUST-001/approve-onboarding
Content-Type: application/json

{
  "officerId": "COMPLIANCE_OFFICER_01",
  "notes": "All identity documents, video KYC, and biometric liveness passed successfully."
}
```

**Response:**
```json
{
  "customerId": "CUST-001",
  "customerNumber": "CUST-984729",
  "fullName": "Alexander Hamilton",
  "email": "a.hamilton@banking-domain.com",
  "status": "ACTIVE",
  "primaryAccountNumber": "ACC5839201847",
  "loginUsername": "a.hamilton@banking-domain.com",
  "temporaryPassword": "pX9#mK2@wL7$",
  "approvedAt": "2026-10-01T23:00:00Z",
  "message": "Customer onboarding approved. Primary account provisioned and credentials generated."
}
```

---

### Sample Walkthrough 1E: Enterprise JWT Authentication & Token Lifecycle with Tamper Prevention

Banking industry standard authentication relies on **Short-Lived JWT Access Tokens** paired with **Cryptographic Refresh Token Rotation (RTR)**.

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Mobile / Web App
    participant Auth as Auth Controller
    participant SecFilter as JwtAuthenticationFilter
    participant BankAPI as Core Banking Services
    participant DB as Customer DB

    Note over Customer, DB: PHASE 1: Authenticate & Obtain Tokens
    Customer->>Auth: POST /api/v1/auth/login { username, password }
    Auth->>DB: Verify BCrypt password & account lock status
    Auth->>DB: Store initial 7-day Refresh Token (rt_...)
    Auth-->>Customer: Return 15-min Access Token (JWT) + Refresh Token

    Note over Customer, BankAPI: PHASE 2: Authenticated API Requests
    Customer->>SecFilter: GET /api/v1/accounts (Header: "Authorization: Bearer <jwt>")
    SecFilter->>SecFilter: Verify HMAC-SHA256 signature, expiry, and extract roles
    SecFilter->>BankAPI: Populate SecurityContextHolder & execute request
    BankAPI-->>Customer: 200 OK (Account balance / Ledger data)

    Note over Customer, Auth: PHASE 3: Access Token Expiry & Refresh Rotation (RTR)
    Customer->>SecFilter: GET /api/v1/accounts (Expired JWT)
    SecFilter-->>Customer: 401 Unauthorized (error="invalid_token")
    Customer->>Auth: POST /api/v1/auth/refresh { refreshToken: "rt_old..." }
    Auth->>DB: Validate active refresh token
    Note over Auth, DB: ROTATE TOKEN: Old token destroyed, brand new token issued!
    Auth->>DB: Update credentials with new refreshToken ("rt_new...")
    Auth-->>Customer: Return new 15-min Access Token + new Rotated Refresh Token

    Note over Customer, Auth: PHASE 4: Anti-Tamper & Replay Attack Defense
    alt Attacker presents already rotated or forged token
        Customer->>Auth: POST /api/v1/auth/refresh { refreshToken: "rt_old..." }
        Auth->>Auth: SECURITY ALERT: Token reuse or forgery detected!
        Auth->>DB: Revoke all active sessions for customer & flag account
        Auth-->>Customer: 401 Unauthorized (Session revoked. Please re-authenticate.)
    end
```

#### 1. Customer Login (`POST /api/v1/auth/login`)
```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "a.hamilton@banking-domain.com",
  "password": "pX9#mK2@wL7$"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJDVVNULTAwMSIsImN1c3RvbWVyTnVtYmVyIjoiQ1VTVC05ODQ3MjkiLCJlbWFpbCI6ImEuaGFtaWx0b25AYmFua2luZy1kb21haW4uY29tIiwidGllciI6IkJBU0lDIiwicm9sZXMiOlsiQ1VTVE9NRVIiLCJUSUVSX0JBU0lDIl0sImlhdCI6MTc5MDk4NTYwMCwiZXhwIjoxNzkwOTg2NTAwLCJqdGkiOiI4ZmQ5ZDAxYy01OTJkLTQ3N2EtOWZjNi1kMGI5ZDFkOGVhMWUifQ.XXXXXX",
  "refreshToken": "rt_9f82c31e9a2b7f6e0c5d4b3a1f8e7d2c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "mustChangePassword": true,
  "customerId": "CUST-001",
  "customerNumber": "CUST-984729",
  "username": "a.hamilton@banking-domain.com",
  "email": "a.hamilton@banking-domain.com",
  "tier": "BASIC",
  "issuedAt": "2026-10-01T23:05:00Z"
}
```

#### 2. Passing JWT Token in Every Protected Request
Every subsequent call to account balance, transfers, card management, or loan applications includes the `Authorization` header:
```http
GET http://localhost:8080/api/v1/accounts/ACC5839201847/balance
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```
The [`JwtAuthenticationFilter.java`](file:///d:/Projects/Resume_Project/banking-common/src/main/java/com/banking/common/security/JwtAuthenticationFilter.java):
- Intercepts the request.
- Validates the cryptographic HMAC-SHA256 signature against the shared secret.
- Checks expiration (`exp` claim).
- Extracts the customer ID, email, tier, and roles (`ROLE_CUSTOMER`, `ROLE_TIER_BASIC`).
- Establishes the authenticated security principal in Spring's `SecurityContextHolder`.

#### 3. Automatic Refresh on Token Expiry & Anti-Tamper Defense
When the 15-minute access token expires, the client's HTTP interceptor receives `401 Unauthorized` and transparently invokes:
```http
POST http://localhost:8080/api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "rt_9f82c31e9a2b7f6e0c5d4b3a1f8e7d2c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e"
}
```

**Anti-Tamper & Replay Attack Defense Logic:**
- **Refresh Token Rotation (RTR):** Every refresh token can only be used **exactly once**. Upon consumption, it is immediately invalidated and a brand new refresh token is issued.
- **Tampered Token Check:** Tokens must match the exact 256-bit high-entropy format (`rt_<hex>`). If tampered with, the request is immediately rejected.
- **Replay / Theft Detection:** If an attacker intercepts and tries to use an already-consumed refresh token, the server detects that the token does not match the active session. It immediately logs a `CRITICAL SECURITY ALERT`, revokes all active refresh tokens for that customer, and forces full credential re-authentication.
- **Brute-Force Lockout:** After 5 failed password attempts, the account is automatically locked (`is_locked = true`).

---

### 👑 Customer Tiers & Privileges Matrix (Industry Standard)

Once a customer completes onboarding and KYC approval, their access to banking features is governed by their **Customer Tier**:

| Tier | Tier Level | Daily Transfer Limit | Max Cards | Accessible Services & Benefits |
| :--- | :---: | :---: | :---: | :--- |
| **`BASIC`** | Standard Retail | **$5,000 / day** | 3 | Core Checking/Savings, domestic transfers (NEFT/IMPS), domestic debit cards, basic statement exports. |
| **`PREMIUM`** | Priority Banking | **$25,000 / day** | 5 | International contactless cards, live cross-border FX quotes, instant UPI, airport lounge passes, fee waivers. |
| **`PLATINUM`** | Wealth & Concierge | **$100,000 / day** | 10 | Preferential forex exchange rates, dedicated relationship manager, pre-approved personal loans, higher daily limits. |
| **`HNI`** | High Net-Worth | **$1,000,000+ / day** | Unlimited | Private wealth management, zero-fee cross-border remittance, luxury metal cards, bespoke credit underwriting. |

---

### 🏷️ Enterprise Microservices Registry (`serviceId`)

Every microservice in this ecosystem is officially registered with a unique, industry-standard `serviceId` (defined in [`BankingServiceRegistry.java`](file:///d:/Projects/Resume_Project/banking-common/src/main/java/com/banking/common/audit/BankingServiceRegistry.java)). Whenever a customer performs an action on any service, the action is logged and audited against this `serviceId`:

| Service Identifier | Official Service Name | Business Domain & Responsibility |
| :---: | :--- | :--- |
| `SRV-GW-000` | **API Gateway** | Entrypoint routing, JWT authentication, Redis token-bucket rate limiting |
| `SRV-ONB-001` | **Customer Onboarding & KYC** | Digital customer identity, AES-256-GCM encrypted ID proofs, V-KYC, customer tiers |
| `SRV-ACC-002` | **Core Banking Ledger** | Double-entry account balances, GraphQL queries, Legacy Mainframe/Finacle SOAP bridge |
| `SRV-PAY-003` | **Payment & Transfers** | Multi-rail fund transfers (UPI, NEFT, IMPS, Cards, FX), 2-Phase Saga Orchestration |
| `SRV-FX-004` | **Forex & Exchange Rates** | ISO-4217 currencies, real-time Brownian motion FX ticker, 60s guaranteed quote locks |
| `SRV-CRD-005` | **Card Management** | Debit/Credit card issuance, Luhn algorithm check, PIN SHA-256 hashing, controls |
| `SRV-LON-006` | **Lending & Loans** | Mathematical EMI formula, credit risk assessment, loan amortization schedules |
| `SRV-FRD-007` | **Fraud Detection** | Real-time sliding-window Redis sorted set velocity rules, transaction risk scoring |
| `SRV-NOT-008` | **Notification Service** | Kafka event listeners, multi-channel alerts (SMS, Email, Push FCM/APNS) |
| `SRV-REP-009` | **Reporting & Statements** | Multi-format exports (PDF, Excel, CSV, JSON), transaction history imports, audit trail |
| `SRV-BTC-010` | **Batch & High-Volume Clearing** | Spring Batch 5 chunk ingestion, Oracle 19c PL/SQL stored procedures, reconciliation |

---

### Sample Walkthrough 1F: Query Customer Dashboard & Permitted Services Catalogue
```http
GET http://localhost:8080/api/v1/customers/CUST-001/dashboard
```
**Response:**
```json
{
  "success": true,
  "data": {
    "customerProfile": {
      "id": "CUST-001",
      "customerNumber": "CN-10001",
      "firstName": "Alexander",
      "lastName": "Hamilton",
      "status": "ACTIVE",
      "customerTier": "PREMIUM"
    },
    "customerTier": "PREMIUM",
    "dailyTransferLimit": 25000.00,
    "maxCardsAllowed": 5,
    "internationalAccess": true,
    "accessibleServices": [
      {
        "serviceId": "SRV-ACC-002",
        "serviceName": "Account & Ledger Service",
        "accessStatus": "GRANTED",
        "enabledFeatures": ["VIEW_BALANCE", "DOWNLOAD_STATEMENT", "OPEN_ACCOUNT"]
      },
      {
        "serviceId": "SRV-PAY-003",
        "serviceName": "Payment & Transfer Service",
        "accessStatus": "GRANTED",
        "enabledFeatures": ["INTERNAL_TRANSFER", "NEFT_TRANSFER", "IMPS_TRANSFER", "UPI_TRANSFER", "PRIORITY_PROCESSING"]
      },
      {
        "serviceId": "SRV-CRD-005",
        "serviceName": "Card Management Service",
        "accessStatus": "GRANTED",
        "enabledFeatures": ["DOMESTIC_CARD", "INTERNATIONAL_CARD", "CONTACTLESS", "AIRPORT_LOUNGE_ACCESS"]
      },
      {
        "serviceId": "SRV-FX-004",
        "serviceName": "Exchange Rate & Forex Service",
        "accessStatus": "GRANTED",
        "enabledFeatures": ["LIVE_FX_TICKER", "CROSS_BORDER_REMITTANCE", "PREFERENTIAL_FX_SPREAD"]
      }
    ]
  }
}
```

---

### Sample Walkthrough 1E: Inspect Universal Customer Action Audit Trail
```http
GET http://localhost:8080/api/v1/customers/CUST-001/actions
```
**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "AUDIT-001",
      "customerId": "CUST-001",
      "serviceId": "SRV-ONB-001",
      "serviceName": "Customer Onboarding & KYC Service",
      "actionType": "CUSTOMER_ONBOARD_INITIATED",
      "details": "Customer profile initiated with tier: PREMIUM",
      "channel": "DIGITAL_ONBOARDING_PORTAL",
      "status": "SUCCESS",
      "timestamp": "2026-10-01T22:50:00Z"
    },
    {
      "id": "AUDIT-002",
      "customerId": "CUST-001",
      "serviceId": "SRV-ACC-002",
      "serviceName": "Account & Ledger Service",
      "actionType": "ACCOUNT_OPENED",
      "details": "Checking account opened with initial deposit $10,000",
      "channel": "CUSTOMER_PORTAL",
      "status": "SUCCESS",
      "timestamp": "2026-10-01T22:53:00Z"
    }
  ]
}
```

---

## 🚶 Step 3: Open an Account & Check Balance (`account-service`)

**Business Story:** With a valid `customerId`, the user opens a Checking or Savings account. This service maintains the double-entry accounting ledger.

### Key Files to Read:
1. [`Account.java`](file:///d:/Projects/Resume_Project/account-service/src/main/java/com/banking/account/domain/Account.java): Tracks `balance` (actual ledger money) and `availableBalance` (balance minus pending holds).
2. [`AccountService.java`](file:///d:/Projects/Resume_Project/account-service/src/main/java/com/banking/account/service/AccountService.java): Notice the `@Transactional` debit and credit methods.
3. [`AccountGraphQLController.java`](file:///d:/Projects/Resume_Project/account-service/src/main/java/com/banking/account/controller/AccountGraphQLController.java): Modern GraphQL query interface alongside REST.
4. [`LegacyCbsMiddlewareGatewayImpl.java`](file:///d:/Projects/Resume_Project/account-service/src/main/java/com/banking/account/middleware/LegacyCbsMiddlewareGatewayImpl.java): Enterprise SOA adapter mediating between Spring Boot and older Mainframe/Finacle systems using SOAP XML with Resilience4j circuit breakers.

### Sample Walkthrough 2: Open an Account
```http
POST http://localhost:8080/api/v1/accounts
Content-Type: application/json

{
  "customerId": "CUST-001",
  "accountType": "CHECKING",
  "currency": "USD",
  "initialDeposit": 10000.00
}
```
**Response:**
```json
{
  "success": true,
  "data": {
    "accountNumber": "US1000000001",
    "customerId": "CUST-001",
    "balance": 10000.00,
    "availableBalance": 10000.00,
    "currency": "USD",
    "status": "ACTIVE"
  }
}
```

### Sample Walkthrough 3: Query Balance via GraphQL
```http
POST http://localhost:8080/graphql
Content-Type: application/json

{
  "query": "{ accountByNumber(accountNumber: \"US1000000001\") { balance availableBalance currency status } }"
}
```
**Response:**
```json
{
  "data": {
    "accountByNumber": {
      "balance": 10000.00,
      "availableBalance": 10000.00,
      "currency": "USD",
      "status": "ACTIVE"
    }
  }
}
```

---

## 🚶 Step 4: Issue a Debit/Credit Card (`card-service`)

**Business Story:** Customers need cards linked to their bank account for ATM withdrawals and online shopping.

### Key Files to Read:
1. [`Card.java`](file:///d:/Projects/Resume_Project/card-service/src/main/java/com/banking/card/domain/Card.java): Entity storing encrypted PIN hash and card status.
2. [`CardService.java`](file:///d:/Projects/Resume_Project/card-service/src/main/java/com/banking/card/service/CardService.java): Notice `generateLuhnCardNumber()`—it creates authentic 16-digit card numbers satisfying the mathematical **ISO/IEC 7812 Luhn Checksum Algorithm**.

---

## 🚶 Step 5: Global Currencies & Foreign Exchange (`exchange-rate-service`)

**Business Story:** If a customer in the US sends USD to a beneficiary in Germany (EUR) or India (INR), what exchange rate applies?

### Key Files to Read:
1. [`DynamicForexRateEngine.java`](file:///d:/Projects/Resume_Project/exchange-rate-service/src/main/java/com/banking/exchange/service/DynamicForexRateEngine.java):
   - Simulates a real interbank forex market using Geometric Brownian Motion ticks every 30 seconds.
   - Computes Bid rate (buy from customer) and Ask rate (sell to customer) with bank spreads.
   - Provides guaranteed 60-second quote locks (`/quote`).

### Sample Walkthrough 4: Get a Live Cross-Currency Transfer Quote
```http
GET http://localhost:8080/api/v1/exchange-rates/quote?fromCurrency=USD&toCurrency=EUR&amount=1000.00
```
**Response:**
```json
{
  "success": true,
  "data": {
    "quoteId": "QTE-7A9B1C2D",
    "sourceCurrency": "USD",
    "targetCurrency": "EUR",
    "sourceAmount": 1000.00,
    "targetAmount": 924.08,
    "exchangeRate": 0.924075,
    "quoteExpiresAt": "2026-10-01T22:45:00Z"
  }
}
```

---

## 🚶 Step 6: Money Transfers & The Saga Orchestrator (`payment-service`)

**Business Story:** This is the heart of the banking platform. When a customer executes an account-to-account transfer, money must be safely debited from the sender's account and credited to the recipient's account across distributed microservices.

### ❓ "Where is Transfer Service in the Architecture?"

In enterprise core banking architecture, **all Fund Transfer functionality is housed directly inside [`payment-service`](file:///d:/Projects/Resume_Project/payment-service)**.

Transfers are not separated into a disconnected standalone dummy service because a real-world fund transfer is a distributed payment workflow requiring **2-Phase Saga orchestration**, **multi-rail routing** (UPI, NEFT, IMPS, Cards, PayPal), **idempotency protection**, and **transactional outbox publishing**.

Here is the exact code mapping for transfers in [`payment-service`](file:///d:/Projects/Resume_Project/payment-service):

| Transfer Capability | Exact Source Code File | Role in the Transfer Flow |
| :--- | :--- | :--- |
| **Transfer API Endpoint** | [`PaymentController.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/controller/PaymentController.java) | Exposes `POST /api/v1/payments/transfers` (initiate transfer) and `GET /api/v1/payments/transfers/{id}` |
| **Transfer Domain Entity** | [`Transfer.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/domain/Transfer.java) | Tracks `sagaId`, `idempotencyKey`, `sourceAccount`, `targetAccount`, `amount`, and `status` (`INITIATED`, `DEBITED`, `COMPLETED`, `FAILED`) |
| **Saga Orchestrator** | [`TransferSagaOrchestrator.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/saga/TransferSagaOrchestrator.java) | Coordinates the 2-Phase distributed transaction. If credit fails, automatically issues compensating refund! |
| **Transfer Rails Factory** | [`PaymentGatewayFactory.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/gateway/PaymentGatewayFactory.java) | Dynamically routes transfer to the correct rail: Internal Ledger, UPI, NEFT, IMPS, Cards, or PayPal |
| **Database Persistence** | [`TransferRepository.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/repository/TransferRepository.java) | Stores transfers in Postgres and queries stalled transfers via `findStalledTransfers` |
| **Auto-Recovery Scheduler** | [`PaymentSchedulerService.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/scheduler/PaymentSchedulerService.java) | Background job running every 60s to detect and auto-compensate any transfer stalled due to network drops |
| **Guaranteed Event Outbox** | [`OutboxPublisher.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/outbox/OutboxPublisher.java) | Pushes `payment.transfer.completed` events to Kafka topic `banking.payment.transfers` for notification alerts |

---

### Why not standard `@Transactional` for Transfers?
In microservices, `account-service` and `payment-service` have separate databases! If the debit succeeds but the credit fails, a standard local transaction cannot roll back the remote database.

### The Solution: 2-Phase Saga Pattern + Transactional Outbox Pattern

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Mobile / Web Client
    participant GW as API Gateway (:8080)
    participant PS as Payment & Transfer Service (:8082)
    participant FD as Fraud Detection (:8087)
    participant AS as Account Service (:8081)
    participant DB as Postgres (transfers table)
    participant KF as Kafka Bus
    participant NS as Notification Service (:8088)

    Customer->>GW: POST /api/v1/payments/transfers
    Note over Customer,GW: Header: Idempotency-Key: TXN-12345
    GW->>PS: Route request to Payment Service
    
    rect rgb(240, 245, 255)
    Note over PS,FD: 1. Risk & Fraud Velocity Check
    PS->>FD: Evaluate velocity & geo-location rules
    FD-->>PS: APPROVE (Low Risk Score: 12)
    end

    rect rgb(245, 255, 245)
    Note over PS,AS: 2. Phase 1 of Saga: Debit Sender
    PS->>DB: Save Transfer status = INITIATED
    PS->>AS: Debit source account ($500)
    AS-->>PS: 200 OK (Source account debited)
    PS->>DB: Update Transfer status = DEBITED
    end

    rect rgb(255, 250, 240)
    Note over PS,AS: 3. Phase 2 of Saga: Credit Recipient
    PS->>AS: Credit target account ($500)
    alt Credit Succeeds
        AS-->>PS: 200 OK (Target credited)
        PS->>DB: Update Transfer status = COMPLETED
        PS->>DB: Save outbox_events record
    else Credit Fails (e.g. Target Frozen / Network Crash)
        Note over PS,AS: Compensating Action (Automatic Refund)
        PS->>AS: Refund $500 back to source account
        PS->>DB: Mark Transfer status = COMPENSATED (FAILED)
        PS->>DB: Save outbox_events (TRANSACTION_COMPENSATED)
    end
    end

    rect rgb(250, 245, 255)
    Note over PS,NS: 4. Asynchronous Outbox Event
    PS->>KF: Publish "payment.transfer.completed" or "compensated"
    KF->>NS: Consume event -> Send SMS & Push Alert to Customer
    end

    PS-->>Customer: Return Final Transfer Status
```

---

### 🛡️ Deep Dive: How Rollbacks and Refunds Work (Compensating Transactions)

In distributed banking architectures, there is no physical "undo" or `ROLLBACK` for an HTTP request that already committed in another microservice's database. Instead, the system executes a **Compensating Transaction**:

#### 1. Transfer State Machine Transitions:
```
           ┌──────────────┐
           │  INITIATED   │
           └──────┬───────┘
                  │ (Source account debited successfully)
                  ▼
           ┌──────────────┐
           │   DEBITED    │
           └──────┬───────┘
                  │
        ┌─────────┴────────────────────────┐
        │ (Credit Target Success)          │ (Credit Target Fails: e.g., Frozen account)
        ▼                                  ▼
 ┌──────────────┐                  ┌──────────────┐
 │  COMPLETED   │                  │ COMPENSATING │
 └──────────────┘                  └──────┬───────┘
                                          │ (Compensating action: Re-credit source account)
                                          ▼
                                   ┌──────────────┐
                                   │ COMPENSATED  │
                                   │   (FAILED)   │
                                   └──────────────┘
```

#### 2. Synchronous Compensation (In-Flight Failure):
In [`TransferSagaOrchestrator.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/saga/TransferSagaOrchestrator.java#L126-L155):
1. **Debit Succeeds:** Source account balance is reduced by `$500.00`. The transfer record transitions to `DEBITED`.
2. **Credit Fails:** The call to credit the destination account throws an exception (e.g., target account is frozen, closed, or network times out).
3. **Catch Block Executes:** The orchestrator marks the transfer as `COMPENSATING`.
4. **Automatic Refund:** The orchestrator immediately calls `accountClient.credit(sourceAccount, 500.00)` to return the money to the sender.
5. **Mark COMPENSATED:** The transfer status is updated to `COMPENSATED` and `markFailed("Credit failed, compensated: ...")`.
6. **Outbox Notification:** A `TRANSACTION_COMPENSATED` event is written to the `outbox_events` table so Kafka can notify the user via SMS: *"Your transfer of $500 could not be delivered and has been refunded to your account."*

#### 3. Asynchronous Compensation (Mid-Flight Network Crash Recovery):
What happens if the server crashes after debiting, before compensation can run?
- [`PaymentSchedulerService.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/scheduler/PaymentSchedulerService.java#L41-L66) runs a background sweep every 60 seconds.
- It queries `transferRepository.findStalledTransfers(cutoff)` for any transfer stuck in `DEBITED` or `INITIATED` status for longer than 5 minutes.
- If a transfer was debited but never reached `COMPLETED`, the scheduler automatically initiates the refund and records the audit reason: `"GATEWAY_TIMEOUT: Auto-compensated by payment recovery scheduler"`.

---

### Sample Walkthrough 5: Execute an Idempotent Fund Transfer
```http
POST http://localhost:8080/api/v1/payments/transfers
Content-Type: application/json
Idempotency-Key: TXN-IDEMP-982173491823

{
  "sourceAccount": "US1000000001",
  "targetAccount": "US2000000002",
  "amount": 500.00,
  "currency": "USD",
  "paymentRail": "INTERNAL",
  "narration": "Monthly rent share"
}
```
**Response:**
```json
{
  "success": true,
  "data": {
    "transferId": "TXN-7F89B10",
    "sagaId": "SAGA-4C5D6E",
    "sourceAccount": "US1000000001",
    "targetAccount": "US2000000002",
    "amount": 500.00,
    "currency": "USD",
    "status": "COMPLETED",
    "message": "Transfer processed and verified through 2-Phase Saga"
  }
}
```

---

## 🚶 Step 7: Fraud Detection & Notifications (`fraud` & `notification`)

### Fraud Detection (`fraud-detection-service`)
- **Key File:** [`RiskAssessmentService.java`](file:///d:/Projects/Resume_Project/fraud-detection-service/src/main/java/com/banking/fraud/service/RiskAssessmentService.java)
- Uses **Redis Sorted Sets (`ZADD`, `ZCOUNT`)** to calculate transaction velocity (e.g., how many transactions occurred in the last 60 seconds).
- Scores risk from 0 (Safe) to 100 (High Risk). Transactions over $10,000 or high velocity trigger an automatic `HOLD` or `REJECT`.

### Notifications (`notification-service`)
- **Key File:** [`PaymentEventConsumer.java`](file:///d:/Projects/Resume_Project/notification-service/src/main/java/com/banking/notification/kafka/PaymentEventConsumer.java)
- Consumes Kafka topic `banking.payment.transfers`.
- Automatically prepares and dispatches SMS, Email, and Push notifications to the customer.

---

## 🚶 Step 8: Statements, Export/Import & Clearing Recon (`reporting` & `batch`)

### Reporting Service (`reporting-service`)
- **Key File:** [`TransactionReportingFacade.java`](file:///d:/Projects/Resume_Project/reporting-service/src/main/java/com/banking/reporting/facade/TransactionReportingFacade.java)
- Uses the **Strategy Pattern** to export statements in 4 industry formats:
  - `PDF`: Formatted customer statement via OpenPDF.
  - `EXCEL`: Multi-column styled workbook via Apache POI 5.3 `.xlsx`.
  - `CSV`: RFC-4180 compliant comma-separated file.
  - `JSON`: RESTful data export.
- Also supports **Import**: Bulk upload of historical CSV/Excel transactions via [`ImportParserFactory.java`](file:///d:/Projects/Resume_Project/reporting-service/src/main/java/com/banking/reporting/importing/ImportParserFactory.java).

### Sample Walkthrough 6: Download Account Statement as PDF or Excel
```http
GET http://localhost:8080/api/v1/reports/export?accountNumber=US1000000001&format=PDF
```
Downloads: `statement_US1000000001.pdf`

```http
GET http://localhost:8080/api/v1/reports/export?accountNumber=US1000000001&format=EXCEL
```
Downloads: `statement_US1000000001.xlsx`

### Batch Clearing & Oracle 19c Reconciliation (`batch-service`)
- **Key Files:**
  - [`ClearingBatchJobConfig.java`](file:///d:/Projects/Resume_Project/batch-service/src/main/java/com/banking/batch/config/ClearingBatchJobConfig.java): Spring Batch 5 chunk-oriented processing streaming 500,000+ clearing records.
  - [`OracleProcedureService.java`](file:///d:/Projects/Resume_Project/batch-service/src/main/java/com/banking/batch/procedure/OracleProcedureService.java): Invokes Oracle 19c stored procedures (`PKG_BANKING_CORE.SP_ACCRUE_DAILY_SAVINGS_INTEREST` and `PKG_RECONCILIATION_ENGINE.SP_RUN_AUTOMATED_RECON`).
  - [`ExactReferenceReconciliationStrategy.java`](file:///d:/Projects/Resume_Project/batch-service/src/main/java/com/banking/batch/reconciliation/strategy/ExactReferenceReconciliationStrategy.java): Matches internal bank ledger records against external clearing feeds (Visa/Mastercard/SWIFT), isolating discrepancies into **Reconciliation Breaks** with automated resolution.


---

## 🚶 Step 9: Wealth Products — Term / Fixed Deposits (`account-service`)

Core banks don't just provide current and checking accounts; they provide high-yield investment instruments: **Term Deposits (Fixed Deposits / Time Deposits)**.

### How Term Deposits Work in Modern Banking:
1. **Tenor & Tiered Interest Rates:**
   - `< 6 months`: 4.50% p.a.
   - `6 – 12 months`: 5.50% p.a.
   - `12 – 24 months`: 6.50% p.a.
   - `> 24 months`: 7.00% p.a.
2. **Compound Interest Formula:**
   $$A = P \times \left(1 + \frac{r}{n}\right)^{n \times t}$$
   - $P$ = Principal amount locked
   - $r$ = Annual nominal interest rate (e.g. 0.065)
   - $n$ = Compounding frequency (Quarterly compounding: $n = 4$)
   - $t$ = Tenor in years ($\text{tenorMonths} / 12.0$)
3. **Premature Liquidation & Early Withdrawal Penalty:**
   - In real-world banking, if a customer breaks their Fixed Deposit before maturity date, a penalty (e.g. `1.0%`) is deducted from the rate, and interest is recalculated strictly for the actual days elapsed.
   - The principal plus discounted accrued interest is refunded into the customer's linked savings account.

### Key File to Study:
- [`TermDepositService.java`](file:///d:/Projects/Resume_Project/account-service/src/main/java/com/banking/account/service/TermDepositService.java)
- [`TermDeposit.java`](file:///d:/Projects/Resume_Project/account-service/src/main/java/com/banking/account/domain/TermDeposit.java)

### Sample Walkthrough 7: Create a Fixed / Term Deposit
```http
POST http://localhost:8080/api/v1/accounts/term-deposits
Content-Type: application/json

{
  "customerId": "CUST-001",
  "linkedAccountNumber": "US1000000001",
  "principalAmount": 10000.00,
  "tenorMonths": 12
}
```
**Response:**
```json
{
  "success": true,
  "data": {
    "depositNumber": "TD1084920491",
    "customerId": "CUST-001",
    "linkedAccountNumber": "US1000000001",
    "principalAmount": 10000.00,
    "interestRate": 0.0650,
    "tenorMonths": 12,
    "compoundingFrequency": "QUARTERLY",
    "maturityAmount": 10666.02,
    "startDate": "2026-10-01",
    "maturityDate": "2027-10-01",
    "status": "ACTIVE"
  }
}
```

---

## 🚶 Step 10: Recurring Payments & Standing Instructions (`payment-service`)

Customers need hands-off automation for recurring monthly bills (rent, gym subscriptions, utility bills, savings sweeps, loan EMIs).

### Architecture & Cron Scheduling:
1. **Instruction Setup:** Customer configures source account, beneficiary account, recurring amount, and frequency (`DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `ANNUALLY`).
2. **Automated Batch Processing:** 
   - Every morning at **06:00 AM** (`0 0 6 * * *`), [`PaymentSchedulerService.java`](file:///d:/Projects/Resume_Project/payment-service/src/main/java/com/banking/payment/scheduler/PaymentSchedulerService.java) queries all `ACTIVE` instructions whose `nextExecutionDate <= today`.
   - Each instruction is submitted to the **Distributed Saga Orchestrator** with a deterministic idempotency key (`si_exec_{id}_{date}`) preventing duplicate charges.
   - Upon successful settlement, `advanceExecutionDate()` pushes the date forward by the frequency interval.

### Sample Walkthrough 8: Set Up a Monthly Rent Standing Instruction
```http
POST http://localhost:8080/api/v1/payments/standing-instructions
Content-Type: application/json

{
  "instructionName": "Monthly Apartment Rent",
  "customerId": "CUST-001",
  "sourceAccountNumber": "US1000000001",
  "targetAccountNumber": "US2000000002",
  "amount": 2500.00,
  "currency": "USD",
  "frequency": "MONTHLY",
  "executionDay": 1,
  "startDate": "2026-11-01",
  "category": "RENT"
}
```

---

## 🚶 Step 11: Card Management & Mobile Banking Controls (`card-service`)

Modern mobile banking apps (Revolut, Monzo, Chase) give customers real-time control over their physical and virtual cards.

### Core Capabilities:
1. **Luhn Algorithm & PAN Generation:** Generates valid 16-digit Primary Account Numbers across Visa, Mastercard, RuPay, and AMEX.
2. **Channel Toggles (Real-Time Fraud Prevention):**
   - `isOnlineEnabled`: Toggle e-commerce / online transactions.
   - `isAtmEnabled`: Toggle cash machine withdrawals.
   - `isPosEnabled`: Toggle in-store merchant card swipes.
   - `isContactlessEnabled`: Toggle NFC tap-to-pay.
   - `isInternationalEnabled`: Toggle foreign currency payments.
3. **Instant Freeze / Unfreeze:**
   - Allows users who misplace their wallet to temporarily lock the card in 1 tap from their phone without permanently voiding the card.
4. **Dynamic CVV (dCVV) for Mobile Screens:**
   - Prevents static CVV theft and shoulder surfing.
   - The mobile app requests a rolling 3-digit CVV calculated using a 300-second (5-minute) time-bucket HMAC. After 5 minutes, the code automatically rolls.
5. **Loyalty Rewards & Cash Equivalent:**
   - Tracks reward points earned on card swipes with real-time cashback conversion ($0.02/pt).

### Key Files to Study:
- [`CardService.java`](file:///d:/Projects/Resume_Project/card-service/src/main/java/com/banking/card/service/CardService.java)
- [`CardController.java`](file:///d:/Projects/Resume_Project/card-service/src/main/java/com/banking/card/controller/CardController.java)

### Sample Walkthrough 9: Generate a 5-Minute Dynamic CVV on Mobile Screen
```http
GET http://localhost:8080/api/v1/cards/card_visa_001/dynamic-cvv
```
**Response:**
```json
{
  "success": true,
  "data": {
    "cardId": "card_visa_001",
    "dynamicCvv": "739",
    "validForSeconds": 248,
    "expiresAt": "2026-10-01T23:15:00Z"
  }
}
```

---

## 🚶 Step 12: Lending & Loan Servicing Lifecycle (`loan-service`)

Lending is the core revenue driver for enterprise banks. The platform manages the full credit lifecycle:

```
[Customer Application] ──> [Credit Assessment] ──> [Officer Approval] ──> [Fund Disbursement] ──> [Monthly EMI Repayments / Foreclosure]
```

### Amortization Schedule Generation:
When a loan is disbursed, [`LoanService.java`](file:///d:/Projects/Resume_Project/loan-service/src/main/java/com/banking/loan/service/LoanService.java#L134-L168) generates a month-by-month schedule breaking down:
- **Principal Component:** Fraction of EMI paying down the borrowed principal.
- **Interest Component:** Calculated on declining balance: $\text{Balance} \times \frac{\text{Rate}}{12}$.
- **Remaining Balance:** Declines until reaches `$0.00` upon final installment.

### Premature Loan Foreclosure:
- A customer can pay off the entire outstanding loan early.
- The system generates a **Foreclosure Quote** including:
  - Outstanding principal
  - Accrued daily interest
  - 2% early closure fee
- Upon settlement, all remaining installments are marked `PAID`, and the loan is updated to `CLOSED`.

### Sample Walkthrough 10: Foreclosure Payoff Quote
```http
GET http://localhost:8080/api/v1/loans/loan_001/foreclosure-quote
```
**Response:**
```json
{
  "success": true,
  "data": {
    "loanId": "loan_001",
    "loanAccountNumber": "LN1000000001",
    "outstandingPrincipal": 18500.00,
    "accruedInterest": 138.75,
    "foreclosurePenalty": 370.00,
    "totalPayoffAmount": 19008.75,
    "validUntil": "2026-10-08"
  }
}
```

---

## 🚶 Step 13: Native Mobile App Integration Guide (iOS & Android)

When integrating a native iOS app (Swift / SwiftUI) or Android app (Kotlin / Jetpack Compose), follow this architecture:

### 1. Mobile Integration Architecture:
```
┌────────────────────────────────────────────────────────┐
│     Native Mobile Client (iOS APNs / Android FCM)      │
│  - Secure Enclave / Android Keystore (Biometric Auth) │
│  - Time-based Idempotency-Key UUID Generator           │
└───────────────────────────┬────────────────────────────┘
                            │ HTTPS / TLS 1.3
                            ▼
┌────────────────────────────────────────────────────────┐
│              API Gateway (Port 8080)                   │
│  - JWT Bearer Token Validation                         │
│  - Rate Limiting (100 req/min per device)              │
│  - Universal Customer Action Auditing (serviceId)      │
└───────────────────────────┬────────────────────────────┘
                            │ gRPC / REST Internal
                            ▼
┌────────────────────────────────────────────────────────┐
│      Downstream Banking Microservices (001 - 010)      │
└────────────────────────────────────────────────────────┘
```

### 2. Mobile App Initialization Sequence:
1. **Device Push Token Registration:**
   - On app launch, iOS requests an **APNs device token**; Android requests an **FCM registration token**.
   - The app binds the device to the customer session by calling:
     ```http
     POST http://localhost:8080/api/v1/notifications/devices/register
     Content-Type: application/json

     {
       "customerId": "CUST-001",
       "platform": "IOS",
       "deviceToken": "apns_hex_token_982374982374982734982374982739487293847928374982734",
       "deviceModel": "iPhone 15 Pro",
       "osVersion": "iOS 18.2",
       "appVersion": "2.4.0",
       "biometricKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
     }
     ```
2. **Customer Tier Dashboard Fetch:**
   - App calls `GET /api/v1/customers/{id}/dashboard` to discover:
     - Customer tier (`BASIC`, `PREMIUM`, `PLATINUM`, `HNI`)
     - Allowed features & transfer limits
     - Authorized banking service microservices (`SRV-GW-000` to `SRV-BTC-010`)
3. **Biometric FaceID / TouchID Signing:**
   - High-value transfers ($5,000+) use the device's `biometricKey` to cryptographically sign the payload before transmitting.

---

## 🏛️ Top 6 Design Patterns to Notice in this Codebase

As a fresher, understanding *why* a pattern is used will accelerate your career:

| Design Pattern | Where It Is Used | Why We Used It Here |
| :--- | :--- | :--- |
| **Strategy Pattern** | `PaymentGatewayFactory` & `ExportStrategyFactory` | Allows switching between payment rails (UPI, Card, NEFT) or export formats (PDF, Excel, CSV) without modifying existing code (Open/Closed Principle). |
| **Saga Pattern** | `PaymentSagaOrchestrator` | Solves the distributed transaction problem across multiple microservices without slow, blocking two-phase commits (2PC). |
| **Transactional Outbox** | `OutboxPublisher` & `outbox_events` table | Guarantees that database updates and Kafka message publishing succeed or fail together atomically. |
| **Facade Pattern** | `TransactionReportingFacade` & `ReconciliationFacade` | Hides complex subsystems behind a clean, simple 1-line API call for controllers. |
| **Decorator / AOP** | `IdempotencyAspect` (`@Idempotent`) | Cross-cutting concern: intercepts incoming requests and deduplicates network retries without cluttering business methods. |
| **Factory Pattern** | `ImportParserFactory` & `ReconciliationRuleFactory` | Instantiates appropriate parsers or reconciliation rule strategies based on file extensions or configuration. |

---

## 🚀 How to Run and Experiment Locally

### 1. Start Support Infrastructure
In your terminal, start the required containers (PostgreSQL, Redis, Kafka):
```bash
docker compose up -d banking-db banking-redis banking-kafka
```

### 2. Run a Microservice Locally
Pick any service to start experimenting with, for example `account-service`:
```bash
mvn spring-boot:run -pl account-service
```

### 3. Explore Interactive Swagger Documentation
Open your browser and test APIs interactively:
- **API Gateway**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Account Service**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Payment Service**: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- **Exchange Rates**: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- **Customer Service**: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)
- **Reporting Service**: [http://localhost:8089/swagger-ui.html](http://localhost:8089/swagger-ui.html)
- **Batch Service**: [http://localhost:8090/swagger-ui.html](http://localhost:8090/swagger-ui.html)

---

## 💡 Quick Tips for Freshers
1. **Always look for the Flyway migration files** in `src/main/resources/db/migration/` of each service first. Reading the database table schema will immediately reveal the business domain.
2. **Follow the logs**: Every microservice outputs structured logs with correlation IDs. Follow the `X-Correlation-ID` header across services to trace a request end-to-end!
3. **Inspect the sample seed data**: All services include sample data in `V2` or `V3` migrations, so you can immediately test with accounts like `US1000000001` without manual setup.
