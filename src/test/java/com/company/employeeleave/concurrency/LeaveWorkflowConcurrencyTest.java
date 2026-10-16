package com.company.employeeleave.concurrency;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.LeaveBalance;
import com.company.employeeleave.domain.entity.LeaveRequest;
import com.company.employeeleave.domain.entity.UserAccount;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.domain.enums.LeaveStatus;
import com.company.employeeleave.domain.enums.LeaveType;
import com.company.employeeleave.domain.enums.Role;
import com.company.employeeleave.repository.DepartmentRepository;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.LeaveBalanceRepository;
import com.company.employeeleave.repository.LeaveRequestRepository;
import com.company.employeeleave.repository.UserAccountRepository;
import com.company.employeeleave.service.LeaveWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LeaveWorkflowConcurrencyTest {

    @Autowired
    private LeaveWorkflowService leaveWorkflowService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private Long managerId;
    private Long employeeId;
    private Long leaveRequestId;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        userAccountRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        Department dept = departmentRepository.save(
                Department.builder().code("TECH").name("Technology").active(true).build()
        );

        Employee manager = employeeRepository.save(
                Employee.builder()
                        .employeeCode("MGR-001")
                        .firstName("Sarah")
                        .lastName("Director")
                        .email("sarah.director@company.com")
                        .jobTitle("Engineering Director")
                        .department(dept)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2022, 1, 1))
                        .build()
        );
        managerId = manager.getId();

        Employee employee = employeeRepository.save(
                Employee.builder()
                        .employeeCode("DEV-001")
                        .firstName("Tom")
                        .lastName("Engineer")
                        .email("tom.engineer@company.com")
                        .jobTitle("Backend Engineer")
                        .department(dept)
                        .manager(manager)
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.of(2023, 1, 1))
                        .build()
        );
        employeeId = employee.getId();

        userAccountRepository.save(
                UserAccount.builder()
                        .username(manager.getEmail())
                        .passwordHash("hashed")
                        .role(Role.ROLE_MANAGER)
                        .employee(manager)
                        .enabled(true)
                        .build()
        );

        // Balance: 10 days total, 0 used
        leaveBalanceRepository.save(
                LeaveBalance.builder()
                        .employee(employee)
                        .year(2026)
                        .leaveType(LeaveType.ANNUAL)
                        .totalDays(10)
                        .usedDays(0)
                        .build()
        );

        // Pending leave request for 6 days
        LeaveRequest pendingRequest = leaveRequestRepository.save(
                LeaveRequest.builder()
                        .employee(employee)
                        .leaveType(LeaveType.ANNUAL)
                        .startDate(LocalDate.of(2026, 8, 3))
                        .endDate(LocalDate.of(2026, 8, 10))
                        .durationDays(6)
                        .reason("Family Trip")
                        .status(LeaveStatus.PENDING)
                        .build()
        );
        leaveRequestId = pendingRequest.getId();
    }

    @Test
    @DisplayName("Simultaneous approval attempts cannot deduct balance twice: exactly 1 succeeds, 1 fails")
    void testConcurrentApproval_PreventsDoubleDeduction() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // Block until all threads are ready, then release at once
                    startLatch.await();
                    leaveWorkflowService.approveLeaveRequest(
                            leaveRequestId, managerId, "Approval from thread " + threadIndex
                    );
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failureCount.incrementAndGet();
                    exceptions.add(t);
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        // Start all threads simultaneously!
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // INVARIANTS:
        // 1. Exactly 1 approval succeeded
        assertThat(successCount.get()).isEqualTo(1);
        // 2. Exactly 1 approval failed due to concurrency conflict (OptimisticLocking or InvalidStateTransition)
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(exceptions).isNotEmpty();

        // 3. Database State Check:
        LeaveRequest finalRequest = leaveRequestRepository.findById(leaveRequestId).orElseThrow();
        assertThat(finalRequest.getStatus()).isEqualTo(LeaveStatus.APPROVED);

        LeaveBalance finalBalance = leaveBalanceRepository
                .findByEmployeeIdAndYearAndLeaveType(employeeId, 2026, LeaveType.ANNUAL)
                .orElseThrow();

        // Crucial invariant: Used days must be exactly 6 (duration of 1 request), NEVER 12!
        assertThat(finalBalance.getUsedDays()).isEqualTo(6);
        assertThat(finalBalance.getRemainingDays()).isEqualTo(4);
    }
}
