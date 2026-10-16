package com.company.employeeleave.service;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.entity.UserAccount;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import com.company.employeeleave.domain.enums.Role;
import com.company.employeeleave.dto.AuthRequest;
import com.company.employeeleave.dto.AuthResponse;
import com.company.employeeleave.dto.RegisterEmployeeRequest;
import com.company.employeeleave.exception.DuplicateResourceException;
import com.company.employeeleave.exception.ResourceNotFoundException;
import com.company.employeeleave.repository.DepartmentRepository;
import com.company.employeeleave.repository.EmployeeRepository;
import com.company.employeeleave.repository.UserAccountRepository;
import com.company.employeeleave.security.CustomUserDetails;
import com.company.employeeleave.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       EmployeeRepository employeeRepository,
                       DepartmentRepository departmentRepository,
                       UserAccountRepository userAccountRepository,
                       EmployeeService employeeService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(userDetails.getUsername())
                .role(userDetails.getRole())
                .employeeId(userDetails.getEmployeeId())
                .employeeCode(userDetails.getEmployeeCode())
                .fullName(userDetails.getFullName())
                .expiresInMs(tokenProvider.getExpirationMs())
                .build();
    }

    /**
     * Public self-registration for new employees.
     * Security controls:
     * 1. Role is strictly hardcoded to ROLE_EMPLOYEE (prevents privilege escalation).
     * 2. Employee code is generated server-side (prevents employee ID manipulation).
     * 3. Passwords are hash-encoded using BCrypt.
     */
    @Transactional
    public AuthResponse register(RegisterEmployeeRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (employeeRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        // Generate safe unique employee code: EMP- + 6 digits
        String employeeCode = "EMP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .jobTitle(request.getJobTitle().trim())
                .department(department)
                .manager(null) // Assigned later by Admin / HR
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        UserAccount userAccount = UserAccount.builder()
                .username(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_EMPLOYEE) // Strictly enforce default role
                .employee(savedEmployee)
                .enabled(true)
                .build();

        userAccountRepository.save(userAccount);

        // Initialize default leave balances for current calendar year
        employeeService.initializeLeaveBalances(savedEmployee, LocalDate.now().getYear());

        CustomUserDetails userDetails = new CustomUserDetails(userAccount);
        String token = tokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(userAccount.getUsername())
                .role(userAccount.getRole())
                .employeeId(savedEmployee.getId())
                .employeeCode(savedEmployee.getEmployeeCode())
                .fullName(savedEmployee.getFullName())
                .expiresInMs(tokenProvider.getExpirationMs())
                .build();
    }

    public AuthResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new ResourceNotFoundException("No authenticated user session found");
        }
        return AuthResponse.builder()
                .username(userDetails.getUsername())
                .role(userDetails.getRole())
                .employeeId(userDetails.getEmployeeId())
                .employeeCode(userDetails.getEmployeeCode())
                .fullName(userDetails.getFullName())
                .build();
    }
}
