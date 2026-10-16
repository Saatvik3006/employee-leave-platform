package com.company.employeeleave.security;

import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.LeaveRequest;
import com.company.employeeleave.domain.enums.Role;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.LeaveRequestRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userSecurityService")
public class UserSecurityService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public UserSecurityService(EmployeeRepository employeeRepository, LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public CustomUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }

    public Long getCurrentEmployeeId() {
        CustomUserDetails user = getCurrentUserDetails();
        return user != null ? user.getEmployeeId() : null;
    }

    public boolean isAdmin() {
        CustomUserDetails user = getCurrentUserDetails();
        return user != null && user.getRole() == Role.ROLE_ADMIN;
    }

    public boolean isManager() {
        CustomUserDetails user = getCurrentUserDetails();
        return user != null && (user.getRole() == Role.ROLE_MANAGER || user.getRole() == Role.ROLE_ADMIN);
    }

    /**
     * Checks if the currently authenticated user can view or update employee details.
     * Allowed if:
     * 1. User is an ADMIN
     * 2. User is accessing their own employee profile
     * 3. User is the direct manager of the target employee
     */
    @Transactional(readOnly = true)
    public boolean canAccessEmployee(Long targetEmployeeId) {
        CustomUserDetails current = getCurrentUserDetails();
        if (current == null) {
            return false;
        }
        if (current.getRole() == Role.ROLE_ADMIN) {
            return true;
        }
        if (current.getEmployeeId().equals(targetEmployeeId)) {
            return true;
        }
        return isManagerOf(current.getEmployeeId(), targetEmployeeId);
    }

    /**
     * Verifies that the manager is the direct manager of the target employee.
     */
    @Transactional(readOnly = true)
    public boolean isManagerOf(Long managerEmployeeId, Long targetEmployeeId) {
        if (managerEmployeeId == null || targetEmployeeId == null) {
            return false;
        }
        return employeeRepository.findById(targetEmployeeId)
                .map(emp -> emp.getManager() != null && managerEmployeeId.equals(emp.getManager().getId()))
                .orElse(false);
    }

    /**
     * Verifies if the authenticated manager has authority over a specific leave request.
     * Crucial check: Enforces manager scope (not just having the ROLE_MANAGER role).
     */
    @Transactional(readOnly = true)
    public boolean canManageLeaveRequest(Long leaveRequestId) {
        CustomUserDetails current = getCurrentUserDetails();
        if (current == null) {
            return false;
        }
        if (current.getRole() == Role.ROLE_ADMIN) {
            return true;
        }
        if (current.getRole() != Role.ROLE_MANAGER) {
            return false;
        }

        return leaveRequestRepository.findWithDetailsById(leaveRequestId)
                .map(lr -> {
                    Employee requester = lr.getEmployee();
                    return requester.getManager() != null &&
                            current.getEmployeeId().equals(requester.getManager().getId());
                })
                .orElse(false);
    }

    /**
     * Checks if user can cancel a leave request (Admin or owner of the request).
     */
    @Transactional(readOnly = true)
    public boolean canCancelLeaveRequest(Long leaveRequestId) {
        CustomUserDetails current = getCurrentUserDetails();
        if (current == null) {
            return false;
        }
        if (current.getRole() == Role.ROLE_ADMIN) {
            return true;
        }

        return leaveRequestRepository.findById(leaveRequestId)
                .map(lr -> lr.getEmployee().getId().equals(current.getEmployeeId()))
                .orElse(false);
    }
}
