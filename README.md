# Calculator SOAP & REST Service

A comprehensive Spring Boot 3.5 application providing calculator operations through both SOAP and REST protocols, built with Apache CXF 4.0.3 and Java 21 LTS.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)
![Java](https://img.shields.io/badge/Java-21%20LTS-orange)
![Apache CXF](https://img.shields.io/badge/Apache%20CXF-4.0.3-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-green)

## 🎯 Overview

This project demonstrates a contract-first SOAP web service implementation using WSDL with Apache CXF, wrapped with REST API endpoints and documented using Swagger/OpenAPI. It showcases enterprise-grade features including SOAP headers, faults, holder patterns, and comprehensive testing.

### Key Features

- ✅ **Dual Protocol Support** - SOAP and REST APIs for the same operations
- ✅ **WSDL-First Design** - Contract-first approach with code generation
- ✅ **Holder Pattern** - JAX-WS Holder for IN/OUT/INOUT parameters
- ✅ **Complex Types** - ResponseDetails with timestamp, serverId, processingTime
- ✅ **SOAP Headers** - Metadata in SOAP response headers
- ✅ **SOAP Faults** - ServiceFailoverFault for error handling
- ✅ **Interactive Documentation** - Swagger UI with REST and SOAP info
- ✅ **Comprehensive Testing** - 16 test cases with 100% success rate
- ✅ **Latest Technology Stack** - Spring Boot 3.5, Java 21, CXF 4.0.3

---

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [API Operations](#-api-operations)
- [API Endpoints](#-api-endpoints)
- [Testing](#-testing)
- [Documentation](#-documentation)
- [Building & Running](#-building--running)
- [Configuration](#-configuration)
- [License](#-license)

---

## 🚀 Quick Start

### Prerequisites

- **Java 21 LTS** or higher
- **Maven 3.9+**
- **Git** (optional)

### Clone & Build

```bash
# Clone the repository
git clone <repository-url>
cd ApacheCFXHelloWord

# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

### Access the Services

Once the application is running:

| Service | URL |
|---------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **SOAP WSDL** | http://localhost:8080/services/Calculator?wsdl |
| **SOAP Endpoint** | http://localhost:8080/services/Calculator |
| **REST API** | http://localhost:8080/api/calculator/* |
| **OpenAPI Docs** | http://localhost:8080/v3/api-docs |

---

## 🛠 Technology Stack

### Core Framework
- **Spring Boot** 3.5.0 - Application framework
- **Java** 21 LTS - Programming language
- **Maven** 3.9.11 - Build tool

### SOAP/Web Services
- **Apache CXF** 4.0.3 - JAX-WS SOAP framework
- **Jakarta XML Web Services** 4.0.0 - JAX-WS API
- **Jakarta XML Binding (JAXB)** - XML marshalling/unmarshalling

### API Documentation
- **SpringDoc OpenAPI** 2.7.0 - Swagger/OpenAPI integration
- **Swagger UI** - Interactive API documentation

### Testing
- **JUnit 5** - Unit testing
- **Spring Boot Test** - Integration testing
- **Maven Surefire** 3.5.3 - Test execution

---

## 📁 Project Structure

```
ApacheCFXHelloWord/
├── src/
│   ├── main/
│   │   ├── java/com/example/cxf/
│   │   │   ├── Application.java                    # Spring Boot main class
│   │   │   ├── config/
│   │   │   │   ├── CalculatorConfig.java          # CXF endpoint configuration
│   │   │   │   └── SwaggerConfig.java             # Swagger/OpenAPI configuration
│   │   │   ├── controller/
│   │   │   │   ├── CalculatorRestController.java  # REST API wrapper
│   │   │   │   └── SoapInfoController.java        # SOAP documentation endpoints
│   │   │   └── service/impl/
│   │   │       └── CalculatorServiceImpl.java     # SOAP service implementation
│   │   └── resources/
│   │       ├── application.yml                     # Application configuration
│   │       └── wsdl/
│   │           ├── Calculator.wsdl                 # SOAP service contract
│   │           └── README.md                       # WSDL documentation
│   └── test/
│       └── java/com/example/cxf/
│           └── ApplicationTests.java               # Integration tests
├── test-requests/                                  # SOAP test request files
│   ├── calculate-add.xml
│   ├── calculate-multiply.xml
│   ├── calculate-divide.xml
│   └── ... (16 test files)
├── target/
│   └── generated-sources/cxf/                      # Generated SOAP classes
│       └── com/example/generated/calculator/
│           ├── CalculatorPortType.java             # SOAP interface
│           ├── AddRequest.java                     # Request holders
│           ├── ResponseDetails.java                # Complex types
│           └── ... (17 generated classes)
├── pom.xml                                         # Maven configuration
├── README.md                                       # This file
├── SWAGGER_DOCUMENTATION.md                        # Swagger guide
├── TEST_RESULTS.md                                 # Test execution results
└── WSDL_UPDATE_SUMMARY.md                          # WSDL structure details
```

---

## 🔧 API Operations

### Calculator Operations

| Operation | Description | Parameters | Return Type |
|-----------|-------------|------------|-------------|
| **add** | Add two integers | `a` (int), `b` (int) | `int` |
| **subtract** | Subtract two integers | `a` (int), `b` (int) | `int` |
| **multiply** | Multiply two integers | `a` (int), `b` (int) | `int` |
| **divide** | Divide two integers | `a` (int), `b` (int) | `double` |
| **calculate** | Dynamic calculation with metadata | `operand1` (int), `operand2` (int), `operation` (string INOUT) | `result` (double OUT), `responseDetails` (OUT) |

### Calculate Operation Details

The `calculate` operation is the most advanced, demonstrating:
- **INOUT parameter** - `operation` (normalized to uppercase)
- **OUT parameters** - `result` and `responseDetails`
- **Complex type response** - ResponseDetails with:
  - `timestamp` - XMLGregorianCalendar (processing time)
  - `serverId` - String (hostname + thread ID)
  - `processingTime` - long (execution time in milliseconds)

---

## 🌐 API Endpoints

### SOAP Endpoints

```
WSDL: http://localhost:8080/services/Calculator?wsdl
Endpoint: http://localhost:8080/services/Calculator
```

**Example SOAP Request:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" 
               xmlns:cal="http://example.com/calculator">
   <soap:Body>
      <cal:calculate>
         <cal:operand1>15</cal:operand1>
         <cal:operand2>10</cal:operand2>
         <cal:operation>ADD</cal:operation>
      </cal:calculate>
   </soap:Body>
</soap:Envelope>
```

**Example SOAP Response:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <calculateResponse xmlns="http://example.com/calculator">
         <result>25.0</result>
         <operation>ADD</operation>
         <responseDetails>
            <timestamp>2026-01-10T12:00:00.123+05:30</timestamp>
            <serverId>HOSTNAME-54</serverId>
            <processingTime>2</processingTime>
         </responseDetails>
      </calculateResponse>
   </soap:Body>
</soap:Envelope>
```

### REST API Endpoints

#### Calculator Operations
```
GET  /api/calculator/add?a={a}&b={b}
GET  /api/calculator/subtract?a={a}&b={b}
GET  /api/calculator/multiply?a={a}&b={b}
GET  /api/calculator/divide?a={a}&b={b}
POST /api/calculator/calculate
GET  /api/calculator/health
```

**Example REST Request:**
```bash
curl "http://localhost:8080/api/calculator/add?a=10&b=5"
```

**Example REST Response:**
```json
{
  "operation": "ADD",
  "operand1": 10,
  "operand2": 5,
  "result": 15
}
```

#### SOAP Service Information
```
GET /api/soap/services                      # Complete SOAP documentation
GET /api/soap/calculator/wsdl-url          # Direct WSDL URL
GET /api/soap/calculator/operations        # Operation details
GET /api/soap/calculator/test-requests     # Sample SOAP envelopes
```

---

## 🧪 Testing

### Test Coverage

| Category | Count | Status |
|----------|-------|--------|
| **Total Test Cases** | 16 | ✅ 100% Pass |
| **Positive Tests** | 13 | ✅ All Pass |
| **Error Tests** | 3 | ✅ All Pass |
| **Operations Tested** | 5 | ✅ Complete |

### Test Files

Located in `test-requests/` directory:

**Calculate Method Tests:**
- `calculate-add.xml` - Addition operation
- `calculate-subtract.xml` - Subtraction operation
- `calculate-multiply.xml` - Multiplication operation
- `calculate-divide.xml` - Clean division
- `calculate-divide-decimal.xml` - Decimal division
- `calculate-negative-numbers.xml` - Negative operands
- `calculate-case-insensitive.xml` - Lowercase operation
- `calculate-divide-by-zero.xml` - Error: division by zero ❌
- `calculate-invalid-operation.xml` - Error: unsupported operation ❌

**Basic Operation Tests:**
- `calculator-add.xml`
- `calculator-subtract.xml`
- `calculator-multiply.xml`
- `calculator-divide.xml`

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean verify

# Skip tests during build
mvn clean package -DskipTests
```

### Testing with PowerShell

```powershell
# Test SOAP operation
Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" `
  -Method POST `
  -Headers @{"Content-Type"="text/xml"; "SOAPAction"="calculate"} `
  -InFile "test-requests/calculate-add.xml" `
  -UseBasicParsing | Select-Object -ExpandProperty Content

# Test REST operation
curl "http://localhost:8080/api/calculator/add?a=10&b=5"
```

### Testing with SoapUI/Postman

1. Import WSDL: `http://localhost:8080/services/Calculator?wsdl`
2. Use test request files from `test-requests/` directory
3. Or get samples from Swagger: `/api/soap/calculator/test-requests`

---

## 📚 Documentation

### Available Documentation Files

| File | Description |
|------|-------------|
| **README.md** | This file - project overview and getting started |
| **SWAGGER_DOCUMENTATION.md** | Swagger UI access guide and REST API docs |
| **TEST_RESULTS.md** | Complete test execution results and analysis |
| **WSDL_UPDATE_SUMMARY.md** | WSDL structure and holder pattern details |
| **test-requests/TEST_CALCULATE_README.md** | Calculate method testing guide |
| **src/main/resources/wsdl/README.md** | WSDL file documentation |

### Interactive Documentation

**Swagger UI:** http://localhost:8080/swagger-ui.html

Features:
- Try out REST API operations directly
- View complete SOAP service information
- See request/response schemas
- Get sample SOAP envelopes
- Access WSDL URL

---

## 🏗 Building & Running

### Maven Commands

```bash
# Clean and build
mvn clean package

# Run application
mvn spring-boot:run

# Generate sources only (from WSDL)
mvn generate-sources

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build without tests
mvn clean package -DskipTests

# Create executable JAR
mvn clean package
java -jar target/ApacheCFXHelloWord.jar
```

### IDE Setup

#### IntelliJ IDEA
1. Open project: `File > Open > pom.xml`
2. Wait for Maven import
3. Run `Application.java` main method

#### Eclipse
1. Import project: `File > Import > Maven > Existing Maven Projects`
2. Select project directory
3. Run as Spring Boot App

#### VS Code
1. Open project folder
2. Install extensions: Java Extension Pack, Spring Boot Extension Pack
3. Run with Spring Boot Dashboard

---

## ⚙️ Configuration

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: apache-cxf-spring-boot

cxf:
  path: /ws
  servlet:
    init:
      service-list-path: /info
```

### Key Configuration Points

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | Application HTTP port |
| `cxf.path` | /ws | CXF servlet path |
| `cxf.servlet.init.service-list-path` | /info | Service listing path |

### Custom Configuration

To change port or other settings, modify `src/main/resources/application.yml`:

```yaml
server:
  port: 8090  # Change port

logging:
  level:
    org.apache.cxf: DEBUG  # Enable CXF debug logging
```

---

## 🔍 Key Features Explained

### Holder Pattern

The WSDL uses holder pattern for flexible parameter passing:

```java
// IN parameter - value provided by client
int operand1

// OUT parameter - value returned to client
Holder<Double> result

// INOUT parameter - value sent and returned
Holder<String> operation
```

### SOAP Headers

ResponseDetails is included in SOAP response headers:
- **timestamp** - When the operation was processed
- **serverId** - Server identifier for tracing
- **processingTime** - Execution time in milliseconds

### SOAP Faults

ServiceFailoverFault provides structured error information:
- **faultCode** - Error code
- **faultMessage** - Human-readable error
- **failoverNode** - Alternative service node
- **retryAfter** - Suggested retry delay

### Code Generation

Apache CXF generates 17 Java classes from WSDL:
- **CalculatorPortType.java** - Service interface
- **Request/Response holders** - For each operation
- **Complex types** - ResponseDetails, faults
- **Service classes** - CalculatorService, ObjectFactory

Generated code location: `target/generated-sources/cxf/`

---

## 🚦 Common Use Cases

### 1. Testing Calculator via REST
```bash
# Add numbers
curl "http://localhost:8080/api/calculator/add?a=10&b=5"

# Calculate with metadata
curl -X POST "http://localhost:8080/api/calculator/calculate" \
  -H "Content-Type: application/json" \
  -d '{"operand1":15,"operand2":10,"operation":"ADD"}'
```

### 2. Getting SOAP Documentation
```bash
# Get all SOAP services info
curl "http://localhost:8080/api/soap/services" | jq

# Get WSDL URL
curl "http://localhost:8080/api/soap/calculator/wsdl-url"

# Get sample SOAP requests
curl "http://localhost:8080/api/soap/calculator/test-requests"
```

### 3. Testing SOAP Service
```powershell
# Using PowerShell
Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" `
  -Method POST `
  -Headers @{"Content-Type"="text/xml"; "SOAPAction"="add"} `
  -InFile "test-requests/calculator-add.xml" `
  -UseBasicParsing
```

### 4. Viewing Interactive Documentation
```
Open browser: http://localhost:8080/swagger-ui.html
- Navigate to "Calculator API" section
- Try "GET /api/calculator/add"
- Click "Try it out"
- Enter values and execute
```

---

## 🎓 Learning Resources

### Understanding WSDL
- [W3C WSDL Specification](https://www.w3.org/TR/wsdl20/)
- See `WSDL_UPDATE_SUMMARY.md` for this project's WSDL structure

### Apache CXF
- [Apache CXF Documentation](https://cxf.apache.org/docs/)
- [JAX-WS Guide](https://cxf.apache.org/docs/developing-a-service.html)

### Spring Boot
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Boot 3.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes)

### Swagger/OpenAPI
- [SpringDoc Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'feat: Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

---

## 👥 Authors

**Calculator Service Team**
- Email: support@example.com
- Website: http://example.com

---

## 🙏 Acknowledgments

- Apache CXF team for the excellent SOAP framework
- Spring Boot team for the amazing framework
- SpringDoc team for Swagger integration
- All contributors and testers

---

## 📞 Support

For issues, questions, or contributions:
- **Issues:** Open a GitHub issue
- **Email:** support@example.com
- **Documentation:** See markdown files in project root

---

## 🔄 Version History

### v1.0.0 (2026-01-10)
- ✅ Initial release
- ✅ Spring Boot 3.5.0 with Java 21 LTS
- ✅ Apache CXF 4.0.3 SOAP service
- ✅ 5 calculator operations (add, subtract, multiply, divide, calculate)
- ✅ WSDL-first with holder pattern
- ✅ ResponseDetails complex type with metadata
- ✅ SOAP headers and faults
- ✅ REST API wrapper
- ✅ Swagger/OpenAPI documentation
- ✅ 16 comprehensive test cases
- ✅ Complete documentation suite

---

**Built with ❤️ using Spring Boot, Apache CXF, and Java 21**
