package com.company.employeeleave.controller;

import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.dto.EmployeeCreateRequest;
import com.company.employeeleave.dto.EmployeeResponse;
import com.company.employeeleave.dto.EmployeeUpdateRequest;
import com.company.employeeleave.dto.LeaveBalanceResponse;
import com.company.employeeleave.dto.PagedResponse;
import com.company.employeeleave.security.UserSecurityService;
import com.company.employeeleave.service.EmployeeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Employee management and profile endpoints")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final LeaveWorkflowService leaveWorkflowService;
    private final UserSecurityService userSecurityService;

    public EmployeeController(EmployeeService employeeService,
                              LeaveWorkflowService leaveWorkflowService,
                              UserSecurityService userSecurityService) {
        this.employeeService = employeeService;
        this.leaveWorkflowService = leaveWorkflowService;
        this.userSecurityService = userSecurityService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current employee profile", description = "Returns profile of the authenticated employee.")
    public ResponseEntity<EmployeeResponse> getCurrentEmployee() {
        Long currentEmployeeId = userSecurityService.getCurrentEmployeeId();
        return ResponseEntity.ok(employeeService.getEmployeeById(currentEmployeeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create employee", description = "Admin only: Creates a new employee with designated role and credentials.")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return new ResponseEntity<>(employeeService.createEmployee(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@userSecurityService.canAccessEmployee(#id)")
    @Operation(summary = "Get employee by ID", description = "Accessible by employee self, direct manager, or Admin.")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Search employees", description = "Manager/Admin only: Search and filter employees with pagination.")
    public ResponseEntity<PagedResponse<EmployeeResponse>> searchEmployees(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(employeeService.searchEmployees(departmentId, status, search, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update employee", description = "Admin only: Updates employee details and department/manager assignments.")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                           @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate employee", description = "Admin only: Deactivates an employee and disables their login account.")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/balances")
    @PreAuthorize("@userSecurityService.canAccessEmployee(#id)")
    @Operation(summary = "Get employee leave balances", description = "Returns annual leave balances for employee.")
    public ResponseEntity<List<LeaveBalanceResponse>> getEmployeeLeaveBalances(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveWorkflowService.getLeaveBalances(id, targetYear));
    }
}
