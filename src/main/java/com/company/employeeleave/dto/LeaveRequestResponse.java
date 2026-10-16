package com.company.employeeleave.dto;

import com.company.employeeleave.domain.enums.LeaveStatus;
import com.company.employeeleave.domain.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveRequestResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationDays;
    private String reason;
    private LeaveStatus status;
    private String decisionReason;
    private Long decidedById;
    private String decidedByName;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    public LeaveRequestResponse() {
    }

    public LeaveRequestResponse(Long id, Long employeeId, String employeeName, String employeeCode,
                                String departmentName, LeaveType leaveType, LocalDate startDate,
                                LocalDate endDate, int durationDays, String reason, LeaveStatus status,
                                String decisionReason, Long decidedById, String decidedByName,
                                LocalDateTime decidedAt, LocalDateTime createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.employeeCode = employeeCode;
        this.departmentName = departmentName;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.durationDays = durationDays;
        this.reason = reason;
        this.status = status;
        this.decisionReason = decisionReason;
        this.decidedById = decidedById;
        this.decidedByName = decidedByName;
        this.decidedAt = decidedAt;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String employeeCode;
        private String departmentName;
        private LeaveType leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private int durationDays;
        private String reason;
        private LeaveStatus status;
        private String decisionReason;
        private Long decidedById;
        private String decidedByName;
        private LocalDateTime decidedAt;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder employeeName(String employeeName) {
            this.employeeName = employeeName;
            return this;
        }

        public Builder employeeCode(String employeeCode) {
            this.employeeCode = employeeCode;
            return this;
        }

        public Builder departmentName(String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        public Builder leaveType(LeaveType leaveType) {
            this.leaveType = leaveType;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder durationDays(int durationDays) {
            this.durationDays = durationDays;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder status(LeaveStatus status) {
            this.status = status;
            return this;
        }

        public Builder decisionReason(String decisionReason) {
            this.decisionReason = decisionReason;
            return this;
        }

        public Builder decidedById(Long decidedById) {
            this.decidedById = decidedById;
            return this;
        }

        public Builder decidedByName(String decidedByName) {
            this.decidedByName = decidedByName;
            return this;
        }

        public Builder decidedAt(LocalDateTime decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LeaveRequestResponse build() {
            return new LeaveRequestResponse(id, employeeId, employeeName, employeeCode,
                    departmentName, leaveType, startDate, endDate, durationDays, reason,
                    status, decisionReason, decidedById, decidedByName, decidedAt, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Long getDecidedById() {
        return decidedById;
    }

    public void setDecidedById(Long decidedById) {
        this.decidedById = decidedById;
    }

    public String getDecidedByName() {
        return decidedByName;
    }

    public void setDecidedByName(String decidedByName) {
        this.decidedByName = decidedByName;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
