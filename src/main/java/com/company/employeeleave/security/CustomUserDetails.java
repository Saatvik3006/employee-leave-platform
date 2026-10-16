package com.company.employeeleave.security;

import com.company.employeeleave.domain.entity.UserAccount;
import com.company.employeeleave.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long userAccountId;
    private final Long employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String username;
    private final String password;
    private final Role role;
    private final Long managerId;
    private final Long departmentId;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(UserAccount userAccount) {
        this.userAccountId = userAccount.getId();
        this.employeeId = userAccount.getEmployee().getId();
        this.employeeCode = userAccount.getEmployee().getEmployeeCode();
        this.fullName = userAccount.getEmployee().getFullName();
        this.username = userAccount.getUsername();
        this.password = userAccount.getPasswordHash();
        this.role = userAccount.getRole();
        this.managerId = userAccount.getEmployee().getManager() != null ? userAccount.getEmployee().getManager().getId() : null;
        this.departmentId = userAccount.getEmployee().getDepartment() != null ? userAccount.getEmployee().getDepartment().getId() : null;
        this.enabled = userAccount.isEnabled();
        this.authorities = List.of(new SimpleGrantedAuthority(userAccount.getRole().name()));
    }

    public Long getUserAccountId() {
        return userAccountId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Long getManagerId() {
        return managerId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
