package com.company.employeeleave.service;

import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.LeaveBalance;
import com.company.employeeleave.domain.entity.LeaveRequest;
import com.company.employeeleave.domain.entity.LeaveWorkflowAudit;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.domain.enums.LeaveStatus;
import com.company.employeeleave.dto.LeaveApplicationRequest;
import com.company.employeeleave.dto.LeaveAuditResponse;
import com.company.employeeleave.dto.LeaveBalanceResponse;
import com.company.employeeleave.dto.LeaveRequestResponse;
import com.company.employeeleave.dto.PagedResponse;
import com.company.employeeleave.exception.BusinessRuleException;
import com.company.employeeleave.exception.InsufficientLeaveBalanceException;
import com.company.employeeleave.exception.InvalidStateTransitionException;
import com.company.employeeleave.exception.ResourceNotFoundException;
import com.company.employeeleave.exception.UnauthorizedOperationException;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.LeaveBalanceRepository;
import com.company.employeeleave.repository.LeaveRequestRepository;
import com.company.employeeleave.repository.LeaveWorkflowAuditRepository;
import com.company.employeeleave.security.UserSecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveWorkflowService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveWorkflowAuditRepository auditRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveCalculationService leaveCalculationService;
    private final UserSecurityService userSecurityService;

    public LeaveWorkflowService(LeaveRequestRepository leaveRequestRepository,
                                LeaveBalanceRepository leaveBalanceRepository,
                                LeaveWorkflowAuditRepository auditRepository,
                                EmployeeRepository employeeRepository,
                                LeaveCalculationService leaveCalculationService,
                                UserSecurityService userSecurityService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.auditRepository = auditRepository;
        this.employeeRepository = employeeRepository;
        this.leaveCalculationService = leaveCalculationService;
        this.userSecurityService = userSecurityService;
    }

    /**
     * Submit a new leave request.
     * Validates date ordering, excludes weekends/holidays, prevents overlapping leaves,
     * and verifies sufficient preliminary balance.
     */
    @Transactional
    public LeaveRequestResponse submitLeaveRequest(Long employeeId, LeaveApplicationRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessRuleException("Inactive employees cannot submit leave requests.");
        }

        int durationDays = leaveCalculationService.calculateWorkingDays(request.getStartDate(), request.getEndDate());

        // Check for overlapping active or pending leave requests
        boolean hasOverlap = leaveRequestRepository.existsOverlappingLeave(
                employeeId, request.getStartDate(), request.getEndDate(), null
        );
        if (hasOverlap) {
            throw new BusinessRuleException(
                    String.format("You already have a pending or approved leave request overlapping with %s to %s.",
                            request.getStartDate(), request.getEndDate())
            );
        }

        // Verify that the employee has sufficient balance for the start year
        int leaveYear = request.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(
                employeeId, leaveYear, request.getLeaveType()
        ).orElseThrow(() -> new InsufficientLeaveBalanceException(
                String.format("No leave balance record found for employee %s, year %d, leave type %s.",
                        employee.getEmployeeCode(), leaveYear, request.getLeaveType())
        ));

        if (!balance.hasSufficientBalance(durationDays)) {
            throw new InsufficientLeaveBalanceException(
                    String.format("Insufficient leave balance for %s: requested %d days, but only %d days remaining.",
                            request.getLeaveType(), durationDays, balance.getRemainingDays())
            );
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .durationDays(durationDays)
                .reason(request.getReason().trim())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        recordAudit(saved, LeaveStatus.PENDING, LeaveStatus.PENDING,
                employee.getEmail(), "Leave request submitted by employee");

        return mapToResponse(saved);
    }

    /**
     * Approves a pending leave request within an atomic transaction.
     * Enforces:
     * 1. Manager scope verification (Manager must manage this employee, or be Admin).
     * 2. State transition check (Only PENDING can be APPROVED).
     * 3. IN-TRANSACTION BALANCE REVALIDATION: Rechecks current balance and deducts usedDays.
     * 4. Concurrency safety: @Version optimistic locking prevents race conditions.
     */
    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long leaveRequestId, Long managerEmployeeId, String comments) {
        LeaveRequest leaveRequest = leaveRequestRepository.findWithDetailsById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveRequestId));

        Employee employee = leaveRequest.getEmployee();

        // 1. Verify manager scope
        verifyManagerAuthority(managerEmployeeId, employee);

        // 2. Verify state transition
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot approve leave request #%d: current status is %s (only PENDING requests can be approved).",
                            leaveRequestId, leaveRequest.getStatus())
            );
        }

        // 3. Revalidate balance inside the same transaction
        int leaveYear = leaveRequest.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(
                employee.getId(), leaveYear, leaveRequest.getLeaveType()
        ).orElseThrow(() -> new InsufficientLeaveBalanceException(
                "Leave balance record not found for employee " + employee.getEmployeeCode()
        ));

        // Atomically deduct balance (throws InsufficientLeaveBalanceException if balance changed)
        balance.deduct(leaveRequest.getDurationDays());
        leaveBalanceRepository.save(balance);

        Employee manager = employeeRepository.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + managerEmployeeId));

        LeaveStatus previousStatus = leaveRequest.getStatus();
        leaveRequest.approve(manager, comments);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        recordAudit(saved, previousStatus, LeaveStatus.APPROVED,
                manager.getEmail(), comments != null ? comments : "Approved by manager");

        return mapToResponse(saved);
    }

    /**
     * Rejects a pending leave request.
     */
    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long leaveRequestId, Long managerEmployeeId, String reason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findWithDetailsById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveRequestId));

        Employee employee = leaveRequest.getEmployee();

        // 1. Verify manager scope
        verifyManagerAuthority(managerEmployeeId, employee);

        // 2. Verify state transition
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot reject leave request #%d: current status is %s (only PENDING requests can be rejected).",
                            leaveRequestId, leaveRequest.getStatus())
            );
        }

        Employee manager = employeeRepository.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + managerEmployeeId));

        LeaveStatus previousStatus = leaveRequest.getStatus();
        leaveRequest.reject(manager, reason);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        recordAudit(saved, previousStatus, LeaveStatus.REJECTED,
                manager.getEmail(), reason != null ? reason : "Rejected by manager");

        return mapToResponse(saved);
    }

    /**
     * Cancels a leave request.
     * Rules:
     * - PENDING: Cancelled immediately (no balance restoration needed as it wasn't deducted).
     * - APPROVED: Can only be cancelled if start date is strictly in the future.
     *   The deducted balance is atomically restored within the same transaction.
     */
    @Transactional
    public LeaveRequestResponse cancelLeaveRequest(Long leaveRequestId, Long currentEmployeeId, String reason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findWithDetailsById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveRequestId));

        Employee employee = leaveRequest.getEmployee();

        // Check ownership or admin privilege
        boolean isOwner = employee.getId().equals(currentEmployeeId);
        boolean isAdmin = userSecurityService.isAdmin();
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException("You are not authorized to cancel this leave request.");
        }

        LeaveStatus previousStatus = leaveRequest.getStatus();

        if (previousStatus == LeaveStatus.APPROVED) {
            // Must be in the future to cancel
            if (!leaveRequest.getStartDate().isAfter(LocalDate.now())) {
                throw new InvalidStateTransitionException(
                        String.format("Cannot cancel approved leave request #%d: leave start date (%s) is today or in the past.",
                                leaveRequestId, leaveRequest.getStartDate())
                );
            }

            // Restore the deducted balance
            int leaveYear = leaveRequest.getStartDate().getYear();
            leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(
                    employee.getId(), leaveYear, leaveRequest.getLeaveType()
            ).ifPresent(balance -> {
                balance.restore(leaveRequest.getDurationDays());
                leaveBalanceRepository.save(balance);
            });
        }

        leaveRequest.cancel(reason);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        recordAudit(saved, previousStatus, LeaveStatus.CANCELLED,
                employee.getEmail(), reason != null ? reason : "Cancelled by requester");

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveRequestById(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + id));
        return mapToResponse(leaveRequest);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestResponse> getLeaveRequestsForEmployee(Long employeeId, Pageable pageable) {
        Page<LeaveRequestResponse> page = leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                .map(this::mapToResponse);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestResponse> getManagerLeaveQueue(Long managerId, LeaveStatus status, Pageable pageable) {
        Page<LeaveRequestResponse> page = leaveRequestRepository.findByManagerScope(managerId, status, pageable)
                .map(this::mapToResponse);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestResponse> getAllLeaveRequests(LeaveStatus status, Pageable pageable) {
        Page<LeaveRequestResponse> page = leaveRequestRepository.findAllWithDetails(status, pageable)
                .map(this::mapToResponse);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getLeaveBalances(Long employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .map(balance -> LeaveBalanceResponse.builder()
                        .id(balance.getId())
                        .employeeId(balance.getEmployee().getId())
                        .employeeName(balance.getEmployee().getFullName())
                        .year(balance.getYear())
                        .leaveType(balance.getLeaveType())
                        .totalDays(balance.getTotalDays())
                        .usedDays(balance.getUsedDays())
                        .remainingDays(balance.getRemainingDays())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveAuditResponse> getLeaveAuditHistory(Long leaveRequestId) {
        return auditRepository.findByLeaveRequestIdOrderByActionTimestampDesc(leaveRequestId).stream()
                .map(audit -> LeaveAuditResponse.builder()
                        .id(audit.getId())
                        .leaveRequestId(audit.getLeaveRequest().getId())
                        .previousStatus(audit.getPreviousStatus())
                        .newStatus(audit.getNewStatus())
                        .actionBy(audit.getActionBy())
                        .actionTimestamp(audit.getActionTimestamp())
                        .comments(audit.getComments())
                        .build())
                .toList();
    }

    private void verifyManagerAuthority(Long managerEmployeeId, Employee employee) {
        if (userSecurityService.isAdmin()) {
            return; // Admins have organization-wide authority
        }

        if (employee.getManager() == null || !employee.getManager().getId().equals(managerEmployeeId)) {
            throw new UnauthorizedOperationException(
                    String.format("Manager ID %d is not authorized to decide leave requests for employee %s (%s). Employee reports to manager ID %s.",
                            managerEmployeeId, employee.getEmployeeCode(), employee.getFullName(),
                            employee.getManager() != null ? employee.getManager().getId() : "None")
            );
        }
    }

    private void recordAudit(LeaveRequest leaveRequest, LeaveStatus previous, LeaveStatus next,
                             String actionBy, String comments) {
        LeaveWorkflowAudit audit = LeaveWorkflowAudit.builder()
                .leaveRequest(leaveRequest)
                .previousStatus(previous)
                .newStatus(next)
                .actionBy(actionBy)
                .comments(comments)
                .build();
        auditRepository.save(audit);
    }

    public LeaveRequestResponse mapToResponse(LeaveRequest lr) {
        return LeaveRequestResponse.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployee().getId())
                .employeeName(lr.getEmployee().getFullName())
                .employeeCode(lr.getEmployee().getEmployeeCode())
                .departmentName(lr.getEmployee().getDepartment() != null ? lr.getEmployee().getDepartment().getName() : null)
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .durationDays(lr.getDurationDays())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .decisionReason(lr.getDecisionReason())
                .decidedById(lr.getDecidedBy() != null ? lr.getDecidedBy().getId() : null)
                .decidedByName(lr.getDecidedBy() != null ? lr.getDecidedBy().getFullName() : null)
                .decidedAt(lr.getDecidedAt())
                .createdAt(lr.getCreatedAt())
                .build();
    }
}
