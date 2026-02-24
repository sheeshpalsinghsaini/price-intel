# PriceIntel Backend

A Spring Boot backend application for a **Price Intelligence and Tracking Platform** that monitors and analyzes product prices across multiple e-commerce platforms.

## 🚀 Tech Stack

- **Java 21**
- **Spring Boot 3.5.11** (MVC, not WebFlux)
- **Spring Data JPA** with Hibernate
- **PostgreSQL 17.8** (port 5433)
- **Lombok** for boilerplate reduction
- **Maven** for dependency management

## 📋 Prerequisites

- Java 21 or higher
- PostgreSQL 17.x running on port 5433
- Maven 3.8+ (or use included Maven wrapper)
- IDE with Lombok support (IntelliJ IDEA recommended)

## 🗄️ Database Setup

### Create Database

```sql
CREATE DATABASE priceintel;
```

### Configuration

Database connection settings are in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/priceintel
    username: postgres
    password: pg@123
```

**Note:** Schema is managed manually. Set `ddl-auto: validate` to ensure entity-schema alignment.

## 🏗️ Project Structure

```
priceintel/
├── src/main/java/io/priceintel/
│   ├── entity/          # JPA entities
│   ├── enums/           # Enums (Availability, CrawlStatus)
│   ├── repository/      # JpaRepository interfaces
│   ├── service/         # Business logic layer
│   ├── controller/      # REST endpoints
│   └── exception/       # Custom exceptions
├── src/main/resources/
│   ├── application.yaml # Configuration
│   └── banner.txt       # Custom startup banner
└── docs/
    └── AI_CONTEXT.md    # Project coding standards
```

## 📦 Core Entities

### Product
- Brand name, product name, pack size
- Unique constraint: `(brand_name, product_name, pack_size)`

### Platform
- E-commerce platform (e.g., Amazon, Flipkart)
- Unique constraint: `name`

### SkuLocation
- Product + Platform + City combination
- Tracks product URL and active status
- Unique constraint: `(product_id, platform_id, city)`

### PriceSnapshot
- Time-series price data
- Captures selling price, discount, availability, crawl status
- Linked to SkuLocation

## 🔧 Build & Run

### Using Maven Wrapper (Recommended)

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Run application
./mvnw spring-boot:run
```

### Using Maven

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run application
mvn spring-boot:run
```

## 📝 Coding Standards

### Database Design
- **Primary Keys:** `BIGSERIAL` in PostgreSQL, `Long` in Java
- **Monetary Values:** Always use `BigDecimal` (never `double`)
- **Timestamps:** Use `Instant` (not `LocalDateTime`)
- **Enums:** Store as `STRING` in database
- **Column Naming:** `snake_case` in DB, `camelCase` in Java
- **Relationships:** Use `FetchType.LAZY` for `@ManyToOne`

### Logging (MANDATORY)
All service classes must use structured logging:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleService {
    
    public Entity create(...) {
        log.debug("Creating entity: param={}", param);
        
        // Validation
        if (invalid) {
            log.warn("Invalid input: {}", reason);
            throw new IllegalArgumentException(...);
        }
        
        // Success
        log.info("Created entity: id={}", entity.getId());
        return entity;
    }
}
```

**Log Levels:**
- `DEBUG`: Method entry with parameters
- `INFO`: Successful operations, business decisions
- `WARN`: Invalid inputs before exceptions
- `ERROR`: Entity not found errors

### Architecture Principles
- ✅ Clean layered architecture (entity → repository → service → controller)
- ✅ Use `@Transactional` for operations involving multiple DB calls
- ✅ Return `Optional<T>` for single entity lookups
- ✅ Custom exceptions for domain errors (e.g., `ProductNotFoundException`)
- ✅ No wildcard imports
- ❌ Never use reactive programming (WebFlux)
- ❌ Never return raw `Map` from controllers
- ❌ Never throw generic `RuntimeException`

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with detailed output
./mvnw test -DtrimStackTrace=false
```

## 🔍 Key Features

### Idempotent Operations
Services implement idempotent patterns:
- `ProductService.createProduct()` - Returns existing if duplicate
- `PlatformService.createPlatform()` - Returns existing if duplicate
- `SkuLocationService.createOrGetSkuLocation()` - Reactivates if inactive

### Duplicate Prevention
`PriceSnapshotService` prevents duplicate snapshots within 30-minute windows when:
- Price, discount, availability, and crawl status are identical
- Reduces storage and maintains data quality

### Smart Reactivation
`SkuLocationService` automatically:
- Reactivates inactive SKU locations
- Updates product URLs when changed
- Preserves historical data

## 📊 API Endpoints

### Products
```
GET  /products          - List all products
GET  /products/{id}     - Get product by ID
```

*(More endpoints will be added as controllers are implemented)*

## 🐛 Troubleshooting

### Common Issues

**Issue:** Schema validation fails
```
Schema-validation: wrong column type encountered...
```
**Solution:** Ensure database schema matches entity definitions. Check `ddl-auto: validate` setting.

**Issue:** Port 5433 already in use
```
Connection refused: localhost:5433
```
**Solution:** Verify PostgreSQL is running on correct port or update `application.yaml`.

**Issue:** Lombok not working in IDE
**Solution:** 
- IntelliJ: Enable annotation processing (Settings → Build → Compiler → Annotation Processors)
- Install Lombok plugin
- Rebuild project

## 📖 Documentation

- **Project Context:** See `docs/AI_CONTEXT.md` for detailed coding standards
- **Entity Relationships:** Check entity classes for JPA mappings
- **Business Logic:** Review service layer for domain rules

## 🤝 Contributing

1. Follow existing code patterns strictly
2. Add `@Slf4j` logging to all service classes
3. Write tests for business logic
4. Use parameterized logging (never string concatenation)
5. Keep commits focused and atomic

## 📄 License

*(Add your license information here)*

## 👥 Team

*(Add team/contact information here)*

---

**Last Updated:** February 24, 2026

