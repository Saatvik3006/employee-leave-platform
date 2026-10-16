package com.company.employeeleave.controller;

import com.company.employeeleave.domain.enums.LeaveStatus;
import com.company.employeeleave.dto.LeaveApplicationRequest;
import com.company.employeeleave.dto.LeaveAuditResponse;
import com.company.employeeleave.dto.LeaveBalanceResponse;
import com.company.employeeleave.dto.LeaveDecisionRequest;
import com.company.employeeleave.dto.LeaveRequestResponse;
import com.company.employeeleave.dto.PagedResponse;
import com.company.employeeleave.security.UserSecurityService;
import com.company.employeeleave.service.LeaveWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@Tag(name = "Leave Management", description = "Leave application, approval workflows, and balance tracking")
@SecurityRequirement(name = "bearerAuth")
public class LeaveController {

    private final LeaveWorkflowService leaveWorkflowService;
    private final UserSecurityService userSecurityService;

    public LeaveController(LeaveWorkflowService leaveWorkflowService, UserSecurityService userSecurityService) {
        this.leaveWorkflowService = leaveWorkflowService;
        this.userSecurityService = userSecurityService;
    }

    @PostMapping
    @Operation(summary = "Submit leave request", description = "Authenticated employee submits a leave request.")
    public ResponseEntity<LeaveRequestResponse> submitLeave(@Valid @RequestBody LeaveApplicationRequest request) {
        Long currentEmployeeId = userSecurityService.getCurrentEmployeeId();
        return new ResponseEntity<>(leaveWorkflowService.submitLeaveRequest(currentEmployeeId, request), HttpStatus.CREATED);
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get my leave requests", description = "Returns paginated list of leave requests for the logged-in employee.")
    public ResponseEntity<PagedResponse<LeaveRequestResponse>> getMyLeaveRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long currentEmployeeId = userSecurityService.getCurrentEmployeeId();
        return ResponseEntity.ok(leaveWorkflowService.getLeaveRequestsForEmployee(currentEmployeeId, pageable));
    }

    @GetMapping("/my-balances")
    @Operation(summary = "Get my leave balances", description = "Returns leave balances for current calendar year for logged-in employee.")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyLeaveBalances(
            @RequestParam(required = false) Integer year) {
        Long currentEmployeeId = userSecurityService.getCurrentEmployeeId();
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveWorkflowService.getLeaveBalances(currentEmployeeId, targetYear));
    }

    @GetMapping("/manager-queue")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get manager approval queue", description = "Managers view leave requests from direct reports. Filter by status.")
    public ResponseEntity<PagedResponse<LeaveRequestResponse>> getManagerQueue(
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Long managerId = userSecurityService.getCurrentEmployeeId();
        return ResponseEntity.ok(leaveWorkflowService.getManagerLeaveQueue(managerId, status, pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all company leave requests", description = "Admin only: View all leave requests across the company.")
    public ResponseEntity<PagedResponse<LeaveRequestResponse>> getAllLeaveRequests(
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(leaveWorkflowService.getAllLeaveRequests(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave request by ID")
    public ResponseEntity<LeaveRequestResponse> getLeaveById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveWorkflowService.getLeaveRequestById(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("@userSecurityService.canManageLeaveRequest(#id)")
    @Operation(summary = "Approve leave request", description = "Direct manager or Admin approves request. Atomically deducts leave balance.")
    public ResponseEntity<LeaveRequestResponse> approveLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveDecisionRequest decision) {
        Long managerId = userSecurityService.getCurrentEmployeeId();
        String comments = decision != null ? decision.getComments() : null;
        return ResponseEntity.ok(leaveWorkflowService.approveLeaveRequest(id, managerId, comments));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("@userSecurityService.canManageLeaveRequest(#id)")
    @Operation(summary = "Reject leave request", description = "Direct manager or Admin rejects request.")
    public ResponseEntity<LeaveRequestResponse> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveDecisionRequest decision) {
        Long managerId = userSecurityService.getCurrentEmployeeId();
        String reason = decision != null ? decision.getComments() : null;
        return ResponseEntity.ok(leaveWorkflowService.rejectLeaveRequest(id, managerId, reason));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("@userSecurityService.canCancelLeaveRequest(#id)")
    @Operation(summary = "Cancel leave request", description = "Employee or Admin cancels request. Atomically restores deducted balance if previously approved.")
    public ResponseEntity<LeaveRequestResponse> cancelLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveDecisionRequest decision) {
        Long currentEmployeeId = userSecurityService.getCurrentEmployeeId();
        String reason = decision != null ? decision.getComments() : null;
        return ResponseEntity.ok(leaveWorkflowService.cancelLeaveRequest(id, currentEmployeeId, reason));
    }

    @GetMapping("/{id}/audits")
    @Operation(summary = "Get leave request workflow history")
    public ResponseEntity<List<LeaveAuditResponse>> getLeaveAuditHistory(@PathVariable Long id) {
        return ResponseEntity.ok(leaveWorkflowService.getLeaveAuditHistory(id));
    }
}
