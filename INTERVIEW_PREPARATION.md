# Java & Spring Boot Interview Preparation Guide
## Based on the Employee Management & Leave Workflow Platform

This guide is designed for an **Associate Software Engineer (~1 Year Experience)** preparing for technical Java/Spring Boot backend interviews. It explains the core concepts, internal mechanisms, failure scenarios, and answers 25+ real enterprise interview questions directly derived from this codebase.

---

## Part 1: Architecture & Key Classes Explained

### 1. `LeaveWorkflowService`
- **Role**: The core domain engine. Handles the state machine transitions (`PENDING` $\to$ `APPROVED` / `REJECTED` / `CANCELLED`), calculates working days, and manages balance adjustments.
- **Why it matters in interviews**: Demonstrates that you know how to write business logic inside the Service layer rather than leaking it into Controllers or SQL queries.
- **Key Methods**:
  - `submitLeaveRequest`: Checks working days, overlaps, and preliminary balances.
  - `approveLeaveRequest`: Enforces manager scope, revalidates balance inside the transaction, deducts balance, and writes audit history atomically.
  - `cancelLeaveRequest`: Restores deducted balance if an approved future leave is cancelled.

### 2. `LeaveCalculationService`
- **Role**: Pure business rule utility. Excludes Saturdays, Sundays, and recognized company public holidays from leave duration.
- **Why it matters in interviews**: Demonstrates separation of concerns. Pure calculation logic is isolated from database access, making it 100% unit-testable without any mocks.

### 3. `UserSecurityService`
- **Role**: Dynamic authorization engine backing `@PreAuthorize("@userSecurityService.canAccessEmployee(#id)")` and `@PreAuthorize("@userSecurityService.canManageLeaveRequest(#id)")`.
- **Why it matters in interviews**: Demonstrates protection against **IDOR (Insecure Direct Object References)**. Most junior engineers only check `hasRole('MANAGER')`, which allows ANY manager to approve ANY employee's leave across the whole company. This class validates the employee's **actual reporting hierarchy**.

### 4. `JwtAuthenticationFilter` & `JwtTokenProvider`
- **Role**: Intercepts incoming HTTP requests, validates the `Authorization: Bearer <token>` header, extracts claims, and populates the `SecurityContextHolder`.
- **Why it matters in interviews**: Demonstrates understanding of stateless REST architecture and the Spring Security Filter Chain.

### 5. `GlobalExceptionHandler`
- **Role**: Centralized `@RestControllerAdvice` converting domain exceptions (`ResourceNotFoundException`, `InsufficientLeaveBalanceException`, `OptimisticLockingFailureException`) into consistent RFC 7807-style JSON error payloads.
- **Why it matters in interviews**: Shows production API maturity — prevents leaking raw stack traces, internal table names, or confusing default 500 errors to clients.

---

## Part 2: Essential Annotations & Mechanics

### `@Transactional`
- **What it does**: Wraps method execution in a database transaction (`BEGIN TRANSACTION` ... `COMMIT` / `ROLLBACK`).
- **How Spring implements it**: Spring creates a **CGLIB dynamic proxy** around the bean. When an external caller invokes the method, the proxy opens a transaction, calls the target method, and commits or rolls back based on whether an unchecked exception (`RuntimeException` or `Error`) was thrown.
- **Interview Trap**: *What happens if a `@Transactional` method calls another `@Transactional` method within the SAME class?*
  - **Answer**: The second method's `@Transactional` annotation is **bypassed** because it is an internal `this.method()` call that does not pass through the Spring proxy. To fix this, move the second method to a separate bean or inject `ApplicationContext` / self-reference.

### `@Version` (Optimistic Locking)
- **What it does**: Appends a version check to SQL updates:
  ```sql
  UPDATE leave_balances 
  SET used_days = ?, version = version + 1 
  WHERE id = ? AND version = ?;
  ```
- **How it prevents race conditions**: If two threads read `version = 0` and both try to update, Thread 1 updates `version = 1`. Thread 2's update matches 0 rows because `version` is now 1. Hibernate detects 0 updated rows and throws `OptimisticLockingFailureException`.

### `@EntityGraph(attributePaths = {"department", "manager"})`
- **What it does**: Tells Hibernate to generate an SQL `LEFT OUTER JOIN` to fetch related entities in a **single query**, overriding `FetchType.LAZY` for that specific query.
- **Why we use it**: It completely eliminates the **N+1 query problem** when fetching lists of employees without forcing global `FetchType.EAGER` in the entity mapping.

### `@EnableMethodSecurity` & `@PreAuthorize`
- **What it does**: Enables method-level authorization using Spring Expression Language (SpEL).
- **How it works**: A Spring AOP method interceptor evaluates the expression before the method executes. If it evaluates to `false`, an `AccessDeniedException` is thrown before the method body runs.

---

## Part 3: Concurrency & Failure Scenarios

### Scenario A: Double Approval Race Condition
- **Problem**: Manager John double-clicks the "Approve" button, firing two simultaneous HTTP requests to approve Alice's 5-day leave.
- **Without Concurrency Protection**: Alice's balance is deducted twice (10 days deducted instead of 5), or the status update overwrites previous decisions.
- **Our Solution**:
  1. Both transactions load `LeaveRequest` with `version = 0`.
  2. Transaction 1 commits first: sets status to `APPROVED`, deducts balance, increments `version` to 1.
  3. Transaction 2 attempts to commit with `version = 0`. The database update fails, Hibernate throws `OptimisticLockingFailureException`.
  4. Centralized exception handler catches this and returns `409 Conflict`.
  5. The balance is deducted **exactly once**.

### Scenario B: Mid-Transaction Failure & Rollback
- **Problem**: In `approveLeaveRequest`, the leave status is updated, but the subsequent audit logging or balance update fails.
- **Our Solution**: Because the entire method is marked `@Transactional`, the JDBC connection issues a `ROLLBACK`. Neither the status change nor the balance update is persisted to MySQL. Data consistency is 100% preserved.

### Scenario C: IDOR Security Exploit
- **Problem**: Employee Alice (ID: 3) inspects the network tab and changes `GET /api/v1/employees/3` to `GET /api/v1/employees/4` (Bob's profile).
- **Our Solution**:
  - The endpoint has `@PreAuthorize("@userSecurityService.canAccessEmployee(#id)")`.
  - `canAccessEmployee` compares the caller's authenticated `employeeId` from the JWT with the URL parameter `targetEmployeeId`.
  - Because `3 != 4` and Alice is neither Bob's manager nor an Admin, the method throws `AccessDeniedException` and returns `403 Forbidden`.

---

## Part 4: 25 Common Java & Spring Boot Interview Questions (With Model Answers)

### Core Java & Concurrency

#### 1. What is the difference between `==` and `.equals()` in Java?
> **Answer**: `==` compares reference memory addresses for objects (or primitive values directly). `.equals()` is a method defined in `java.lang.Object` intended to compare value equality. By default, `Object.equals()` uses `==`, so classes must override it (e.g. `String`, custom POJOs) to compare internal state.

#### 2. Why should you always override `hashCode()` when you override `equals()`?
> **Answer**: The general contract of `hashCode` states that if two objects are equal according to `equals()`, they **must** return the same integer hash code. If you override `equals` without `hashCode`, hash-based collections (`HashMap`, `HashSet`) will place equal objects into different buckets, causing lookup failures and duplicate entries.

#### 3. How does Java's `CountDownLatch` work, and how did you use it in this project?
> **Answer**: A `CountDownLatch` is initialized with a given count. Calling `await()` blocks the calling thread until other threads call `countDown()` enough times to reduce the count to zero. In our `LeaveWorkflowConcurrencyTest`, we used a `startLatch` with count 1 to release multiple threads simultaneously, simulating a true race condition against the leave approval method.

#### 4. What is the difference between Optimistic Locking and Pessimistic Locking?
> **Answer**:
> - **Optimistic Locking**: Assumes collisions are rare. No database locks are held while reading. A version column (`@Version`) verifies that data hasn't changed upon commit. If it changed, `OptimisticLockingFailureException` is thrown.
> - **Pessimistic Locking**: Assumes collisions are frequent. It executes `SELECT ... FOR UPDATE`, acquiring a row-level database lock that blocks other transactions until the current transaction commits or rolls back.

---

### Spring Boot & Spring Framework

#### 5. What is Inversion of Control (IoC) and Dependency Injection (DI)?
> **Answer**: IoC is the design principle where the control of object creation, configuration, and lifecycle is inverted from the application code to a framework container (the Spring IoC container). Dependency Injection is the mechanism implementing IoC: objects receive their dependencies (collaborators) from the container rather than instantiating them with `new`.

#### 6. Why is Constructor Injection preferred over Field Injection (`@Autowired`)?
> **Answer**:
> 1. **Immutability**: Dependencies can be declared `final`, ensuring they cannot be reassigned after creation.
> 2. **Testability**: In unit tests (e.g., using Mockito or plain JUnit), dependencies can be passed directly via the constructor without needing reflection or Spring container startup.
> 3. **Fail-Fast**: Missing dependencies cause compilation errors or clear startup failures, preventing `NullPointerException` at runtime.
> 4. **Prevents Circular Dependencies**: The compiler or container detects circular dependencies immediately at construction time.

#### 7. Explain the Spring Bean lifecycle.
> **Answer**:
> 1. Bean definition loaded and class instantiated via constructor.
> 2. Dependencies injected (setter/property population).
> 3. Aware interfaces processed (`BeanNameAware`, `ApplicationContextAware`).
> 4. `BeanPostProcessor.postProcessBeforeInitialization()`.
> 5. Initialization callback (`@PostConstruct` or `InitializingBean.afterPropertiesSet()`).
> 6. `BeanPostProcessor.postProcessAfterInitialization()` (this is where AOP proxies are created!).
> 7. Bean is ready for use in application context.
> 8. Destruction callback on context shutdown (`@PreDestroy` or `DisposableBean.destroy()`).

#### 8. What is the difference between `@Component`, `@Service`, and `@Repository`?
> **Answer**: `@Component` is the generic archetype for any Spring-managed component. `@Service` and `@Repository` are specialized meta-annotations:
> - `@Service`: Marks business logic classes (documentation/semantic clarity).
> - `@Repository`: Marks data access classes and automatically translates database-specific SQLExceptions into Spring’s `DataAccessException` hierarchy.

---

### Spring Data JPA & Hibernate

#### 9. What is the JPA N+1 query problem, and how did you prevent it?
> **Answer**: The N+1 problem occurs when fetching $N$ parent entities triggers 1 query for the parents and then $N$ additional queries to fetch their lazy relationships (e.g., fetching 20 employees triggers 20 individual queries to fetch their departments).
> In this project, we prevented it by using **`@EntityGraph(attributePaths = {"department", "manager"})`** on repository finder methods and **`JOIN FETCH`** in custom JPQL queries, ensuring the parent and child entities are fetched in a single SQL query.

#### 10. Why should you avoid `FetchType.EAGER` in entity mappings?
> **Answer**: `FetchType.EAGER` forces the persistence provider to fetch the associated entity every single time the parent is queried, regardless of whether it is needed. This leads to massive Cartesian product joins, performance degradation, and cannot be turned off at query time. Best practice is to default all `@ManyToOne` and `@OneToOne` associations to `FetchType.LAZY` and fetch them explicitly via `@EntityGraph` or `JOIN FETCH` when needed.

#### 11. What is Hibernate's "Dirty Checking"?
> **Answer**: Within a transaction, Hibernate tracks the state of entities loaded into the First-Level Cache (Persistence Context). When the transaction ends, Hibernate compares the entity's current state against the original snapshot taken when loaded. If any property has changed, Hibernate automatically issues an `UPDATE` statement without requiring explicit calls to `repository.save()`.

#### 12. Why shouldn't you use Lombok's `@Data` or `@EqualsAndHashCode` on JPA Entities?
> **Answer**:
> 1. `@EqualsAndHashCode` generates equals/hashCode using all fields. If an entity contains lazy associations (`@ManyToOne` or `@OneToMany`), checking equality triggers lazy loading of those associations, potentially causing `LazyInitializationException` or catastrophic N+1 query cascades.
> 2. Generated `toString()` calls on bidirectional relationships cause infinite recursion (`StackOverflowError`).
> 3. An entity's identity is defined by its database primary key (`id`) or business key, not mutable attributes.

#### 13. What is the purpose of Flyway database migrations?
> **Answer**: Flyway provides version-controlled, repeatable, and automated schema migrations. Instead of relying on Hibernate's `ddl-auto=update` (which is dangerous in production, cannot rename columns, and doesn't record history), Flyway maintains a `flyway_schema_history` table and applies SQL scripts (`V1__...`, `V2__...`) sequentially.

---

### Spring Security & Authentication

#### 14. How does the Spring Security Filter Chain work?
> **Answer**: Spring Security is built on standard servlet filters. A single `DelegatingFilterProxy` in the servlet container delegates requests to Spring's `FilterChainProxy`, which executes a list of `SecurityFilterChain` filters in order (e.g., `CorsFilter`, `CsrfFilter`, custom `JwtAuthenticationFilter`, `UsernamePasswordAuthenticationFilter`, `FilterSecurityInterceptor`). Each filter can inspect, modify, or block the request, or populate the `SecurityContext`.

#### 15. Where does Spring Security store authentication information for the current request?
> **Answer**: It is stored in `SecurityContextHolder`, which by default uses a `ThreadLocal` strategy. This binds the `SecurityContext` (containing the `Authentication` object and `UserDetails`) to the executing thread. At the end of request processing, Spring Security cleans up the `ThreadLocal` to prevent thread-pool memory leaks.

#### 16. Why use stateless JWT authentication instead of HTTP Sessions in a REST API?
> **Answer**: Stateless JWT eliminates server-side session memory storage. Because each request carries a self-contained, digitally signed token containing user claims and expiration, any server instance behind a load balancer can validate the request without shared session caches (like Redis) or sticky sessions.

#### 17. How do you prevent Insecure Direct Object References (IDOR) in Spring Boot?
> **Answer**: Never rely solely on URL parameters (e.g. `/api/v1/employees/{id}`). In the service layer or via `@PreAuthorize`, verify that the ID in the request matches `currentAuthenticatedUser.getEmployeeId()`, or verify that the caller is an authorized manager or admin.

---

### Transaction Management & Concurrency

#### 18. What are ACID properties in a database?
> **Answer**:
> - **Atomicity**: All operations in a transaction succeed or all roll back (all-or-nothing).
> - **Consistency**: The database transitions from one valid state to another, obeying all constraints.
> - **Isolation**: Concurrent transactions execute without interfering with one another.
> - **Durability**: Once committed, changes survive server crashes.

#### 19. What exception types trigger a rollback in Spring `@Transactional` by default?
> **Answer**: By default, Spring rolls back transactions only for **unchecked exceptions** (`RuntimeException` and `Error`). It does **not** roll back for checked exceptions (`Exception`) unless explicitly configured with `@Transactional(rollbackFor = Exception.class)`.

#### 20. What is the difference between `Isolation.READ_COMMITTED` and `Isolation.REPEATABLE_READ`?
> **Answer**:
> - `READ_COMMITTED` (MySQL default for some engines, PostgreSQL default): A query only sees changes committed before the query began. Prevents dirty reads, but allows non-repeatable reads (re-reading the same row in the same transaction may yield different values).
> - `REPEATABLE_READ` (MySQL InnoDB default): A transaction sees a consistent snapshot of the database taken when the transaction began. Prevents dirty reads and non-repeatable reads.

#### 21. How does our leave approval implementation guarantee that leave balance is never deducted twice?
> **Answer**:
> 1. It re-validates the balance **inside the same transaction** after acquiring the leave request entity.
> 2. Both `LeaveRequest` and `LeaveBalance` entities contain a `@Version` attribute.
> 3. If two concurrent threads attempt to approve and deduct, the first to commit increments the version. The second thread fails with `OptimisticLockingFailureException`.
> 4. Additionally, the database schema enforces a `CHECK (used_days <= total_days)` constraint, guaranteeing database-level protection.

---

### REST API Design & Error Handling

#### 22. What HTTP status code should be returned for business rule violations?
> **Answer**: HTTP `400 Bad Request` or HTTP `422 Unprocessable Entity` (when syntax is valid but semantic business rules fail). For concurrent state conflicts, HTTP `409 Conflict` is the correct standard code.

#### 23. Why should APIs return DTOs instead of JPA Entities?
> **Answer**:
> 1. **Prevents Over-fetching & Serialization Loops**: Entities with bidirectional relationships cause infinite JSON serialization recursion.
> 2. **Decouples API Contract from DB Schema**: Changing a database table column doesn't break external API clients.
> 3. **Security (Mass Assignment Vulnerability)**: If entities are accepted as `@RequestBody`, malicious users can inject values into fields they shouldn't edit (e.g., setting `status = APPROVED` or altering `role`).

#### 24. What is the role of Jakarta Bean Validation in Spring Boot?
> **Answer**: By placing annotations (`@NotNull`, `@NotBlank`, `@Size`, `@FutureOrPresent`) on DTO fields and annotating controller arguments with `@Valid`, Spring automatically triggers validation before entering the controller method. If validation fails, `MethodArgumentNotValidException` is thrown and captured by `@RestControllerAdvice`.

#### 25. How do you version a REST API, and what approach was taken here?
> **Answer**: Common approaches include URI path versioning (`/api/v1/...`), request headers (`X-API-Version: 1`), or accept header MIME types (`application/vnd.company.v1+json`). In this project, URI path versioning (`/api/v1`) was used because it is transparent, browser-friendly, works seamlessly with Swagger UI, and is simple for client integrations.
