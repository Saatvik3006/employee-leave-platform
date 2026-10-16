package com.company.employeeleave.service;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.LeaveBalance;
import com.company.employeeleave.domain.entity.UserAccount;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.domain.enums.LeaveType;
import com.company.employeeleave.dto.EmployeeCreateRequest;
import com.company.employeeleave.dto.EmployeeResponse;
import com.company.employeeleave.dto.EmployeeUpdateRequest;
import com.company.employeeleave.dto.PagedResponse;
import com.company.employeeleave.exception.BusinessRuleException;
import com.company.employeeleave.exception.DuplicateResourceException;
import com.company.employeeleave.exception.ResourceNotFoundException;
import com.company.employeeleave.repository.DepartmentRepository;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.LeaveBalanceRepository;
import com.company.employeeleave.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           UserAccountRepository userAccountRepository,
                           LeaveBalanceRepository leaveBalanceRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee with email '" + request.getEmail() + "' already exists.");
        }
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new DuplicateResourceException("Employee with code '" + request.getEmployeeCode() + "' already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.getManagerId()));
        }

        Employee employee = Employee.builder()
                .employeeCode(request.getEmployeeCode().toUpperCase().trim())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .jobTitle(request.getJobTitle().trim())
                .department(department)
                .manager(manager)
                .status(EmployeeStatus.ACTIVE)
                .hireDate(request.getHireDate())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        // Create linked UserAccount
        UserAccount userAccount = UserAccount.builder()
                .username(savedEmployee.getEmail())
                .passwordHash(passwordEncoder.encode(request.getInitialPassword()))
                .role(request.getRole())
                .employee(savedEmployee)
                .enabled(true)
                .build();
        userAccountRepository.save(userAccount);

        // Seed initial leave balances for current calendar year
        initializeLeaveBalances(savedEmployee, LocalDate.now().getYear());

        return mapToResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        return mapToResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findWithDetailsByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + email));
        return mapToResponse(employee);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(Long departmentId,
                                                          EmployeeStatus status,
                                                          String search,
                                                          Pageable pageable) {
        Page<EmployeeResponse> page = employeeRepository.searchEmployees(departmentId, status, search, pageable)
                .map(this::mapToResponse);
        return PagedResponse.from(page);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        Employee manager = null;
        if (request.getManagerId() != null) {
            if (request.getManagerId().equals(id)) {
                throw new BusinessRuleException("An employee cannot be their own manager.");
            }
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.getManagerId()));
        }

        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setDepartment(department);
        employee.setManager(manager);

        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        return mapToResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);

        userAccountRepository.findByEmployeeId(id).ifPresent(account -> {
            account.setEnabled(false);
            userAccountRepository.save(account);
        });
    }

    public void initializeLeaveBalances(Employee employee, int year) {
        List<LeaveBalance> balances = List.of(
                LeaveBalance.builder().employee(employee).year(year).leaveType(LeaveType.ANNUAL).totalDays(20).usedDays(0).build(),
                LeaveBalance.builder().employee(employee).year(year).leaveType(LeaveType.SICK).totalDays(10).usedDays(0).build(),
                LeaveBalance.builder().employee(employee).year(year).leaveType(LeaveType.CASUAL).totalDays(5).usedDays(0).build(),
                LeaveBalance.builder().employee(employee).year(year).leaveType(LeaveType.UNPAID).totalDays(30).usedDays(0).build()
        );
        leaveBalanceRepository.saveAll(balances);
    }

    public EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .jobTitle(employee.getJobTitle())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .departmentCode(employee.getDepartment() != null ? employee.getDepartment().getCode() : null)
                .managerId(employee.getManager() != null ? employee.getManager().getId() : null)
                .managerName(employee.getManager() != null ? employee.getManager().getFullName() : null)
                .status(employee.getStatus())
                .hireDate(employee.getHireDate())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}
