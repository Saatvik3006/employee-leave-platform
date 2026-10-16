package com.company.employeeleave.integration;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.LeaveBalance;
import com.company.employeeleave.domain.entity.LeaveRequest;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.domain.enums.LeaveStatus;
import com.company.employeeleave.domain.enums.LeaveType;
import com.company.employeeleave.dto.LeaveApplicationRequest;
import com.company.employeeleave.dto.LeaveRequestResponse;
import com.company.employeeleave.repository.DepartmentRepository;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.LeaveBalanceRepository;
import com.company.employeeleave.repository.LeaveRequestRepository;
import com.company.employeeleave.service.LeaveWorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test running against a real MySQL 8 container via Testcontainers.
 * Validates MySQL-specific behavior, constraints, and Flyway migrations.
 * Tagged with 'integration' so it is excluded from fast local unit test runs and executed via CI/Failsafe.
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
class LeaveWorkflowMySQLIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("test_employee_leave_db")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private LeaveWorkflowService leaveWorkflowService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    @DisplayName("End-to-end leave workflow lifecycle against real MySQL container")
    void testEndToEndWorkflowOnMySQL() {
        Department dept = departmentRepository.save(
                Department.builder().code("OPS").name("Operations").active(true).build()
        );

        Employee manager = employeeRepository.save(
                Employee.builder()
                        .employeeCode("MGR-200")
                        .firstName("Marcus")
                        .lastName("Lead")
                        .email("marcus.lead@company.com")
                        .jobTitle("Operations Manager")
                        .department(dept)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2021, 5, 1))
                        .build()
        );

        Employee employee = employeeRepository.save(
                Employee.builder()
                        .employeeCode("EMP-200")
                        .firstName("Elena")
                        .lastName("Rostova")
                        .email("elena.rostova@company.com")
                        .jobTitle("Cloud Specialist")
                        .department(dept)
                        .manager(manager)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2022, 10, 1))
                        .build()
        );

        leaveBalanceRepository.save(
                LeaveBalance.builder()
                        .employee(employee)
                        .year(2026)
                        .leaveType(LeaveType.ANNUAL)
                        .totalDays(15)
                        .usedDays(0)
                        .build()
        );

        // 1. Submit
        LocalDate start = LocalDate.of(2026, 9, 7);
        LocalDate end = LocalDate.of(2026, 9, 11);
        LeaveRequestResponse submitted = leaveWorkflowService.submitLeaveRequest(
                employee.getId(),
                LeaveApplicationRequest.builder()
                        .leaveType(LeaveType.ANNUAL)
                        .startDate(start)
                        .endDate(end)
                        .reason("Annual Leave Vacation")
                        .build()
        );
        assertThat(submitted.getStatus()).isEqualTo(LeaveStatus.PENDING);

        // 2. Approve
        LeaveRequestResponse approved = leaveWorkflowService.approveLeaveRequest(
                submitted.getId(), manager.getId(), "Approved on MySQL"
        );
        assertThat(approved.getStatus()).isEqualTo(LeaveStatus.APPROVED);

        // Verify balance in MySQL
        LeaveBalance balanceAfterApproval = leaveBalanceRepository
                .findByEmployeeIdAndYearAndLeaveType(employee.getId(), 2026, LeaveType.ANNUAL)
                .orElseThrow();
        assertThat(balanceAfterApproval.getUsedDays()).isEqualTo(5);
        assertThat(balanceAfterApproval.getRemainingDays()).isEqualTo(10);
    }
}
