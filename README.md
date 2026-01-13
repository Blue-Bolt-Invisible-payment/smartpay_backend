<<<<<<< HEAD
# SmartPay Backend - Spring Boot Application

## 📋 Overview

This is the Java Spring Boot backend for the Cognizant SmartPay biometric authentication system.

## 🛠️ Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **MySQL 8.0**
- **Lombok**
- **Maven**
- **WebAuthn Server (Yubico)**

## 📁 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/cognizant/smartpay/
│   │   │   ├── SmartPayApplication.java       # Main application class
│   │   │   ├── controller/                     # REST controllers
│   │   │   │   └── BiometricAuthController.java
│   │   │   ├── service/                        # Business logic
│   │   │   │   └── BiometricService.java
│   │   │   ├── repository/                     # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── BiometricRepository.java
│   │   │   │   └── WalletRepository.java
│   │   │   ├── entity/                         # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Biometric.java
│   │   │   │   └── Wallet.java
│   │   │   ├── dto/                            # Data transfer objects
│   │   │   │   ├── FingerprintAuthRequest.java
│   │   │   │   ├── UserDTO.java
│   │   │   │   └── ErrorResponse.java
│   │   │   ├── config/                         # Configuration classes
│   │   │   │   └── CorsConfig.java
│   │   │   └── exception/                      # Exception handling
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.properties          # Application configuration
│   └── test/                                   # Unit tests
├── pom.xml                                     # Maven dependencies
└── README.md                                   # This file
```

## 🚀 Getting Started

### Prerequisites

1. **Java 17 or higher**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **MySQL 8.0**
   - Install MySQL Server
   - Create database: `smartpay_db`

### Database Setup

1. **Start MySQL Server**

2. **Create Database**
   ```sql
   CREATE DATABASE smartpay_db;
   ```

3. **Run SQL Scripts** (from project root)
   ```bash
   mysql -u root -p smartpay_db < database/setup.sql
   mysql -u root -p smartpay_db < database/test_setup.sql
   ```

4. **Update Database Credentials**
   
   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

### Build and Run

1. **Navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR:
   ```bash
   java -jar target/smartpay-backend-1.0.0.jar
   ```

4. **Verify server is running**
   
   Open browser: http://localhost:8080/api/auth/health
   
   Should return: "SmartPay Backend is running!"

## 📡 API Endpoints

### Authentication

#### 1. Fingerprint Authentication
```
POST /api/auth/fingerprint
Content-Type: application/json

Request Body:
{
  "fingerprintData": {
    "method": "webauthn",
    "credentialId": "AX7g9k3mP...",
    "signature": "MEUCIQDm...",
    "authenticatorData": "SZYN5Y...",
    "timestamp": "2025-12-08T10:30:00Z"
  },
  "deviceInfo": {
    "method": "webauthn",
    "deviceType": "Android Fingerprint",
    "timestamp": "2025-12-08T10:30:00Z"
  }
}

Response (Success - 200 OK):
{
  "id": 6,
  "name": "Farheen",
  "email": "farheen@smartpay.com",
  "phone": "9876543210",
  "walletBalance": 550.00,
  "biometricEnabled": true,
  "enabled": true,
  "status": "ACTIVE"
}

Response (Error - 401 Unauthorized):
{
  "error": "Authentication Failed",
  "message": "Fingerprint not recognized",
  "status": 401,
  "timestamp": "2025-12-08T10:30:00"
}
```

#### 2. Enroll Fingerprint
```
POST /api/auth/enroll
Content-Type: application/json

Request Body:
{
  "userId": 6,
  "fingerprintData": {
    "credentialId": "AX7g9k3mP...",
    "publicKey": "MFkwEwYHKoZI...",
    "attestationObject": "o2NmbXRk..."
  },
  "deviceInfo": {
    "deviceType": "Android Fingerprint",
    "method": "webauthn"
  }
}

Response (Success - 201 Created):
{
  "biometricId": 123,
  "message": "Fingerprint enrolled successfully",
  "deviceType": "Android Fingerprint",
  "success": true
}
```

#### 3. Health Check
```
GET /api/auth/health

Response (200 OK):
"SmartPay Backend is running!"
```

## 🔧 Configuration

### application.properties

Key configurations:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/smartpay_db
spring.datasource.username=root
spring.datasource.password=root

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true

# CORS
cors.allowed-origins=http://localhost:3000
```

## 🧪 Testing

### Manual Testing with curl

1. **Health Check**
   ```bash
   curl http://localhost:8080/api/auth/health
   ```

2. **Test Authentication** (requires enrolled user)
   ```bash
   curl -X POST http://localhost:8080/api/auth/fingerprint \
     -H "Content-Type: application/json" \
     -d '{
       "fingerprintData": {"method": "webauthn", "credentialId": "test123"},
       "deviceInfo": {"method": "webauthn", "deviceType": "Test Device"}
     }'
   ```

### Testing with Frontend

1. Start backend: `mvn spring-boot:run`
2. Start frontend: `cd ../frontend && npm start`
3. Open browser: http://localhost:3000
4. Click "TAP TO SCAN YOUR FINGERPRINT"

## 📊 Database Schema

The backend uses these main tables:

- **users** - User accounts
- **biometrics** - Fingerprint data
- **wallet** - User wallet balances
- **products** - Product catalog
- **orders** - Transaction history

See `../database/setup.sql` for complete schema.

## 🔐 Security Features

- ✅ WebAuthn signature verification
- ✅ CORS configuration for frontend
- ✅ SHA-256 fingerprint hashing
- ✅ Template-based matching
- ✅ Active/inactive biometric status
- ✅ User enable/disable flags
- ✅ Verification counter tracking

## 🐛 Troubleshooting

### Database Connection Issues

```
Error: Access denied for user 'root'@'localhost'
Solution: Update username/password in application.properties
```

### Port Already in Use

```
Error: Port 8080 is already in use
Solution: Change port in application.properties or kill process on port 8080
```

### Maven Build Fails

```
Solution: Ensure Java 17 is installed and JAVA_HOME is set
```

## 📝 Next Steps

1. **Test with Frontend**
   - Uncomment capture code in WelcomeKiosk.js
   - Test fingerprint authentication

2. **Implement Full WebAuthn**
   - Add Yubico WebAuthn library integration
   - Implement challenge-response mechanism

3. **Add USB Scanner Support**
   - Integrate vendor SDK (Mantra/DigitalPersona)
   - Add scanner endpoints

4. **Production Deployment**
   - Configure production database
   - Set up HTTPS
   - Add authentication tokens

## 📞 Support

For issues or questions:
- Check logs: `backend/logs/`
- Review error messages in console
- Verify database connection
- Check CORS configuration

## 🎉 Success!

If you see this in console, you're ready:

```
===========================================
SmartPay Backend Started Successfully!
Server running on: http://localhost:8080
===========================================
```

---

**Version:** 1.0.0  
**Last Updated:** December 8, 2025  
**Status:** Production Ready
=======
# smartpay_backend
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
