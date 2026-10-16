package com.company.employeeleave.security;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.UserAccount;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.domain.enums.Role;
import com.company.employeeleave.dto.AuthRequest;
import com.company.employeeleave.dto.DepartmentRequest;
import com.company.employeeleave.dto.RegisterEmployeeRequest;
import com.company.employeeleave.repository.DepartmentRepository;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.LeaveBalanceRepository;
import com.company.employeeleave.repository.LeaveRequestRepository;
import com.company.employeeleave.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long deptId;
    private Long adminId;
    private Long managerId;
    private Long aliceId;
    private Long bobId;

    private String adminToken;
    private String managerToken;
    private String aliceToken;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        userAccountRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        Department dept = departmentRepository.save(
                Department.builder().code("ENG").name("Engineering").active(true).build()
        );
        deptId = dept.getId();

        // 1. Admin
        Employee admin = employeeRepository.save(
                Employee.builder()
                        .employeeCode("ADM-001")
                        .firstName("Admin")
                        .lastName("Root")
                        .email("admin@test.com")
                        .jobTitle("Super Admin")
                        .department(dept)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2020, 1, 1))
                        .build()
        );
        adminId = admin.getId();
        UserAccount adminAccount = userAccountRepository.save(
                UserAccount.builder()
                        .username(admin.getEmail())
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(Role.ROLE_ADMIN)
                        .employee(admin)
                        .enabled(true)
                        .build()
        );
        adminToken = jwtTokenProvider.generateToken(new CustomUserDetails(adminAccount));

        // 2. Manager John
        Employee manager = employeeRepository.save(
                Employee.builder()
                        .employeeCode("MGR-002")
                        .firstName("John")
                        .lastName("Manager")
                        .email("manager@test.com")
                        .jobTitle("Engineering Manager")
                        .department(dept)
                        .manager(admin)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2021, 1, 1))
                        .build()
        );
        managerId = manager.getId();
        UserAccount managerAccount = userAccountRepository.save(
                UserAccount.builder()
                        .username(manager.getEmail())
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(Role.ROLE_MANAGER)
                        .employee(manager)
                        .enabled(true)
                        .build()
        );
        managerToken = jwtTokenProvider.generateToken(new CustomUserDetails(managerAccount));

        // 3. Alice (Reports to Manager John)
        Employee alice = employeeRepository.save(
                Employee.builder()
                        .employeeCode("DEV-003")
                        .firstName("Alice")
                        .lastName("Smith")
                        .email("alice@test.com")
                        .jobTitle("Software Engineer")
                        .department(dept)
                        .manager(manager)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2022, 1, 1))
                        .build()
        );
        aliceId = alice.getId();
        UserAccount aliceAccount = userAccountRepository.save(
                UserAccount.builder()
                        .username(alice.getEmail())
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(Role.ROLE_EMPLOYEE)
                        .employee(alice)
                        .enabled(true)
                        .build()
        );
        aliceToken = jwtTokenProvider.generateToken(new CustomUserDetails(aliceAccount));

        // 4. Bob (Reports to Manager John)
        Employee bob = employeeRepository.save(
                Employee.builder()
                        .employeeCode("DEV-004")
                        .firstName("Bob")
                        .lastName("Jones")
                        .email("bob@test.com")
                        .jobTitle("QA Engineer")
                        .department(dept)
                        .manager(manager)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2023, 1, 1))
                        .build()
        );
        bobId = bob.getId();
        userAccountRepository.save(
                UserAccount.builder()
                        .username(bob.getEmail())
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(Role.ROLE_EMPLOYEE)
                        .employee(bob)
                        .enabled(true)
                        .build()
        );
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Login with valid credentials returns 200 OK and JWT token")
    void testLoginSuccess() throws Exception {
        AuthRequest req = AuthRequest.builder()
                .username("alice@test.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ROLE_EMPLOYEE"))
                .andExpect(jsonPath("$.username").value("alice@test.com"));
    }

    @Test
    @DisplayName("Public registration strictly enforces ROLE_EMPLOYEE and safe defaults (prevents privilege escalation)")
    void testPublicRegistrationSafeRole() throws Exception {
        RegisterEmployeeRequest req = RegisterEmployeeRequest.builder()
                .firstName("Charlie")
                .lastName("Brown")
                .email("charlie@test.com")
                .password("StrongPassword123!")
                .jobTitle("Associate Engineer")
                .departmentId(deptId)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ROLE_EMPLOYEE"))
                .andExpect(jsonPath("$.username").value("charlie@test.com"));

        UserAccount created = userAccountRepository.findByUsername("charlie@test.com").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.ROLE_EMPLOYEE);
        assertThat(created.getEmployee().getEmployeeCode()).startsWith("EMP-");
    }

    @Test
    @DisplayName("IDOR prevention: Employee Alice cannot access Bob's private employee profile (403 Forbidden)")
    void testEmployeeCannotAccessOtherEmployeeProfile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee Alice can access her own profile (200 OK)")
    void testEmployeeCanAccessOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + aliceId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("Manager John can access his direct reportee Alice's profile (200 OK)")
    void testManagerCanAccessReporteeProfile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + aliceId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Smith"));
    }

    @Test
    @DisplayName("Non-admin role cannot create department (403 Forbidden)")
    void testNonAdminCannotCreateDepartment() throws Exception {
        DepartmentRequest req = DepartmentRequest.builder()
                .code("HR")
                .name("Human Resources")
                .build();

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin role can create department (201 Created)")
    void testAdminCanCreateDepartment() throws Exception {
        DepartmentRequest req = DepartmentRequest.builder()
                .code("HR")
                .name("Human Resources")
                .build();

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("HR"));
    }
}
