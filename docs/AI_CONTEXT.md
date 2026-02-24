PROJECT CONTEXT: PRICEINTEL BACKEND

This is a Spring Boot 3.5.x backend application for a Price Intelligence platform.

Tech Stack:
- Spring Boot (MVC, not WebFlux)
- Spring Data JPA
- PostgreSQL (port 5433)
- Java 21
- Lombok
- Hibernate validation enabled
- ddl-auto = validate (DB schema is manually managed)

Database Design Principles:
- All primary keys use BIGSERIAL in PostgreSQL.
- All entity IDs are Long in Java.
- All monetary values use BigDecimal (never double).
- All timestamps use Instant.
- Enum fields use @Enumerated(EnumType.STRING).
- snake_case column names in DB, camelCase in Java.
- Unique constraints are enforced at DB level and reflected in entity annotations.
- ManyToOne relationships must use FetchType.LAZY.
- No wildcard imports.

Architecture Layers:
- entity → JPA entities
- repository → JpaRepository interfaces
- service → business logic layer
- controller → REST endpoints
- exception → custom exceptions + global exception handler

Error Handling:
- Business exceptions use custom RuntimeException classes.
- GlobalExceptionHandler returns structured ApiErrorResponse DTO.
- Never return raw Map for API response.
- 404 used for not found.
- Do not throw generic RuntimeException.

Coding Rules:
- Follow clean architecture principles.
- Do not modify database schema automatically.
- Do not use WebFlux or reactive programming.
- Do not introduce unnecessary frameworks.
- Keep logic simple and production-grade.
- Always preserve existing structure unless explicitly instructed.

Logging Standards (MANDATORY):
- All service classes MUST use @Slf4j annotation (Lombok).
- Never create manual logger objects.
- Use parameterized logging (never string concatenation).
- Log levels:
  * DEBUG: Method entry with key parameters
  * INFO: Successful operations, business decisions, entity creation/updates
  * WARN: Invalid inputs before throwing IllegalArgumentException
  * ERROR: Entity not found before throwing custom exceptions
- Include relevant context: IDs, names, key business values.
- Keep logs concise and production-ready for troubleshooting.

Current Progress:
- Entities created: Product, Platform, SkuLocation, PriceSnapshot.
- Enums: Availability, CrawlStatus.
- Repositories implemented with required custom finder methods.
- ProductService implemented.
- ProductController GET endpoints working.
- Global exception handling implemented.
- Dummy data inserted and verified.

Goal:
We are incrementally building backend foundation for a scalable price tracking system.
All changes must align with existing database schema and architectural decisions.

Before generating code:
- Do not refactor unrelated files.
- Do not introduce breaking changes.
- Follow existing patterns strictly.