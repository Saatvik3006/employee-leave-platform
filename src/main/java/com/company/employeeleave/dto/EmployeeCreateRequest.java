package com.company.employeeleave.dto;

import com.company.employeeleave.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class EmployeeCreateRequest {

    @NotBlank(message = "Employee code is required")
    @Size(min = 3, max = 50, message = "Employee code must be between 3 and 50 characters")
    private String employeeCode;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email format")
    private String email;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private Long managerId;

    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    private LocalDate hireDate;

    @NotBlank(message = "Initial password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String initialPassword;

    @NotNull(message = "Role is required")
    private Role role;

    public EmployeeCreateRequest() {
    }

    public EmployeeCreateRequest(String employeeCode, String firstName, String lastName, String email,
                                 String jobTitle, Long departmentId, Long managerId, LocalDate hireDate,
                                 String initialPassword, Role role) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.jobTitle = jobTitle;
        this.departmentId = departmentId;
        this.managerId = managerId;
        this.hireDate = hireDate;
        this.initialPassword = initialPassword;
        this.role = role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String employeeCode;
        private String firstName;
        private String lastName;
        private String email;
        private String jobTitle;
        private Long departmentId;
        private Long managerId;
        private LocalDate hireDate;
        private String initialPassword;
        private Role role;

        public Builder employeeCode(String employeeCode) {
            this.employeeCode = employeeCode;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder jobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
            return this;
        }

        public Builder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public Builder managerId(Long managerId) {
            this.managerId = managerId;
            return this;
        }

        public Builder hireDate(LocalDate hireDate) {
            this.hireDate = hireDate;
            return this;
        }

        public Builder initialPassword(String initialPassword) {
            this.initialPassword = initialPassword;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public EmployeeCreateRequest build() {
            return new EmployeeCreateRequest(employeeCode, firstName, lastName, email, jobTitle,
                    departmentId, managerId, hireDate, initialPassword, role);
        }
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
