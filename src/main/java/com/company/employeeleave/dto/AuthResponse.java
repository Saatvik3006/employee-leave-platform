package com.company.employeeleave.dto;

import com.company.employeeleave.domain.enums.Role;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String username;
    private Role role;
    private Long employeeId;
    private String employeeCode;
    private String fullName;
    private long expiresInMs;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, String username, Role role,
                        Long employeeId, String employeeCode, String fullName, long expiresInMs) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.expiresInMs = expiresInMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType = "Bearer";
        private String username;
        private Role role;
        private Long employeeId;
        private String employeeCode;
        private String fullName;
        private long expiresInMs;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder employeeCode(String employeeCode) {
            this.employeeCode = employeeCode;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder expiresInMs(long expiresInMs) {
            this.expiresInMs = expiresInMs;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, tokenType, username, role, employeeId, employeeCode, fullName, expiresInMs);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }
}
