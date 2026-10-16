package com.company.employeeleave.service;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.LeaveBalance;
import com.company.employeeleave.domain.entity.LeaveRequest;
import com.company.employeeleave.domain.entity.LeaveWorkflowAudit;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.domain.enums.LeaveStatus;
import com.company.employeeleave.domain.enums.LeaveType;
import com.company.employeeleave.dto.LeaveApplicationRequest;
import com.company.employeeleave.dto.LeaveRequestResponse;
import com.company.employeeleave.exception.BusinessRuleException;
import com.company.employeeleave.exception.InsufficientLeaveBalanceException;
import com.company.employeeleave.exception.InvalidStateTransitionException;
import com.company.employeeleave.exception.UnauthorizedOperationException;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.LeaveBalanceRepository;
import com.company.employeeleave.repository.LeaveRequestRepository;
import com.company.employeeleave.repository.LeaveWorkflowAuditRepository;
import com.company.employeeleave.security.UserSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveWorkflowServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveWorkflowAuditRepository auditRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveCalculationService leaveCalculationService;

    @Mock
    private UserSecurityService userSecurityService;

    @InjectMocks
    private LeaveWorkflowService leaveWorkflowService;

    private Employee employee;
    private Employee manager;
    private Employee otherManager;
    private LeaveBalance balance;

    @BeforeEach
    void setUp() {
        Department dept = Department.builder().id(1L).name("Engineering").code("ENG").build();

        manager = Employee.builder()
                .id(2L)
                .employeeCode("EMP-002")
                .firstName("John")
                .lastName("Manager")
                .email("manager.john@company.com")
                .department(dept)
                .status(EmployeeStatus.ACTIVE)
                .build();

        otherManager = Employee.builder()
                .id(99L)
                .employeeCode("EMP-099")
                .firstName("Evil")
                .lastName("Manager")
                .email("evil.manager@company.com")
                .department(dept)
                .status(EmployeeStatus.ACTIVE)
                .build();

        employee = Employee.builder()
                .id(3L)
                .employeeCode("EMP-003")
                .firstName("Alice")
                .lastName("Dev")
                .email("alice.dev@company.com")
                .department(dept)
                .manager(manager)
                .status(EmployeeStatus.ACTIVE)
                .build();

        balance = LeaveBalance.builder()
                .id(10L)
                .employee(employee)
                .year(2026)
                .leaveType(LeaveType.ANNUAL)
                .totalDays(20)
                .usedDays(0)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Submit leave request successfully with valid dates and sufficient balance")
    void testSubmitLeaveRequest_Success() {
        LocalDate start = LocalDate.of(2026, 7, 6);
        LocalDate end = LocalDate.of(2026, 7, 10);
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .leaveType(LeaveType.ANNUAL)
                .startDate(start)
                .endDate(end)
                .reason("Summer Vacation")
                .build();

        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(leaveCalculationService.calculateWorkingDays(start, end)).thenReturn(5);
        when(leaveRequestRepository.existsOverlappingLeave(3L, start, end, null)).thenReturn(false);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(3L, 2026, LeaveType.ANNUAL))
                .thenReturn(Optional.of(balance));

        LeaveRequest savedRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(start)
                .endDate(end)
                .durationDays(5)
                .reason("Summer Vacation")
                .status(LeaveStatus.PENDING)
                .version(0L)
                .build();

        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(savedRequest);

        LeaveRequestResponse response = leaveWorkflowService.submitLeaveRequest(3L, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(response.getDurationDays()).isEqualTo(5);
        verify(auditRepository).save(any(LeaveWorkflowAudit.class));
    }

    @Test
    @DisplayName("Submit leave request throws InsufficientLeaveBalanceException when balance is too low")
    void testSubmitLeaveRequest_InsufficientBalance() {
        LocalDate start = LocalDate.of(2026, 7, 6);
        LocalDate end = LocalDate.of(2026, 7, 17);
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .leaveType(LeaveType.ANNUAL)
                .startDate(start)
                .endDate(end)
                .reason("Long Vacation")
                .build();

        balance.setUsedDays(18); // Only 2 remaining out of 20

        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(leaveCalculationService.calculateWorkingDays(start, end)).thenReturn(10);
        when(leaveRequestRepository.existsOverlappingLeave(3L, start, end, null)).thenReturn(false);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(3L, 2026, LeaveType.ANNUAL))
                .thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> leaveWorkflowService.submitLeaveRequest(3L, request))
                .isInstanceOf(InsufficientLeaveBalanceException.class)
                .hasMessageContaining("requested 10 days, but only 2 days remaining");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit leave request throws BusinessRuleException when overlapping leave exists")
    void testSubmitLeaveRequest_OverlappingLeave() {
        LocalDate start = LocalDate.of(2026, 7, 6);
        LocalDate end = LocalDate.of(2026, 7, 10);
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .leaveType(LeaveType.ANNUAL)
                .startDate(start)
                .endDate(end)
                .reason("Vacation")
                .build();

        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(leaveCalculationService.calculateWorkingDays(start, end)).thenReturn(5);
        when(leaveRequestRepository.existsOverlappingLeave(3L, start, end, null)).thenReturn(true);

        assertThatThrownBy(() -> leaveWorkflowService.submitLeaveRequest(3L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("overlapping");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Approve leave request successfully: deducts balance and updates status in transaction")
    void testApproveLeaveRequest_Success() {
        LeaveRequest pendingRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 7, 6))
                .endDate(LocalDate.of(2026, 7, 10))
                .durationDays(5)
                .status(LeaveStatus.PENDING)
                .version(0L)
                .build();

        when(leaveRequestRepository.findWithDetailsById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userSecurityService.isAdmin()).thenReturn(false);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(3L, 2026, LeaveType.ANNUAL))
                .thenReturn(Optional.of(balance));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestResponse response = leaveWorkflowService.approveLeaveRequest(100L, 2L, "Approved, enjoy!");

        assertThat(response.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(balance.getUsedDays()).isEqualTo(5); // Deducted!
        assertThat(balance.getRemainingDays()).isEqualTo(15);
        verify(leaveBalanceRepository).save(balance);
        verify(auditRepository).save(any(LeaveWorkflowAudit.class));
    }

    @Test
    @DisplayName("Approve leave request throws UnauthorizedOperationException when manager is not the employee's manager")
    void testApproveLeaveRequest_UnauthorizedManagerScope() {
        LeaveRequest pendingRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee) // Employee reports to Manager 2L
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 7, 6))
                .endDate(LocalDate.of(2026, 7, 10))
                .durationDays(5)
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findWithDetailsById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userSecurityService.isAdmin()).thenReturn(false);

        // Evil manager 99L tries to approve Alice's leave
        assertThatThrownBy(() -> leaveWorkflowService.approveLeaveRequest(100L, 99L, "Unauthorized approval attempt"))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("not authorized to decide leave requests");

        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("In-transaction balance revalidation: throws InsufficientLeaveBalanceException if balance changed prior to approval")
    void testApproveLeaveRequest_RevalidationFailure() {
        LeaveRequest pendingRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 7, 6))
                .endDate(LocalDate.of(2026, 7, 10))
                .durationDays(5)
                .status(LeaveStatus.PENDING)
                .build();

        balance.setUsedDays(18); // Only 2 days left! But request needs 5 days!

        when(leaveRequestRepository.findWithDetailsById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userSecurityService.isAdmin()).thenReturn(false);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(3L, 2026, LeaveType.ANNUAL))
                .thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> leaveWorkflowService.approveLeaveRequest(100L, 2L, "Approve"))
                .isInstanceOf(InsufficientLeaveBalanceException.class)
                .hasMessageContaining("requested 5 days, but only 2 days remaining");

        assertThat(pendingRequest.getStatus()).isEqualTo(LeaveStatus.PENDING); // Unchanged!
    }

    @Test
    @DisplayName("Approve leave throws InvalidStateTransitionException if request is not PENDING")
    void testApproveLeaveRequest_InvalidStateTransition() {
        LeaveRequest alreadyApproved = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 7, 6))
                .endDate(LocalDate.of(2026, 7, 10))
                .durationDays(5)
                .status(LeaveStatus.APPROVED) // Already approved
                .build();

        when(leaveRequestRepository.findWithDetailsById(100L)).thenReturn(Optional.of(alreadyApproved));
        when(userSecurityService.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> leaveWorkflowService.approveLeaveRequest(100L, 2L, "Approve again"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("only PENDING requests can be approved");
    }

    @Test
    @DisplayName("Cancel APPROVED future leave restores deducted balance")
    void testCancelApprovedFutureLeave_RestoresBalance() {
        // Future date: 2026-11-10
        LeaveRequest approvedRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(25))
                .durationDays(5)
                .status(LeaveStatus.APPROVED)
                .build();

        balance.setUsedDays(5); // Was deducted upon approval

        when(leaveRequestRepository.findWithDetailsById(100L)).thenReturn(Optional.of(approvedRequest));
        when(userSecurityService.isAdmin()).thenReturn(false);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(eq(3L), anyInt(), eq(LeaveType.ANNUAL)))
                .thenReturn(Optional.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequestResponse response = leaveWorkflowService.cancelLeaveRequest(100L, 3L, "Plans changed");

        assertThat(response.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(balance.getUsedDays()).isEqualTo(0); // Restored!
        verify(leaveBalanceRepository).save(balance);
    }
}
