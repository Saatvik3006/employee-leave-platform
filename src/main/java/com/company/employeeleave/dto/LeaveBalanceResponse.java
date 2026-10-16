package com.company.employeeleave.dto;

import com.company.employeeleave.domain.enums.LeaveType;

public class LeaveBalanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private int year;
    private LeaveType leaveType;
    private int totalDays;
    private int usedDays;
    private int remainingDays;

    public LeaveBalanceResponse() {
    }

    public LeaveBalanceResponse(Long id, Long employeeId, String employeeName, int year,
                                LeaveType leaveType, int totalDays, int usedDays, int remainingDays) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.year = year;
        this.leaveType = leaveType;
        this.totalDays = totalDays;
        this.usedDays = usedDays;
        this.remainingDays = remainingDays;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private int year;
        private LeaveType leaveType;
        private int totalDays;
        private int usedDays;
        private int remainingDays;

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

        public Builder year(int year) {
            this.year = year;
            return this;
        }

        public Builder leaveType(LeaveType leaveType) {
            this.leaveType = leaveType;
            return this;
        }

        public Builder totalDays(int totalDays) {
            this.totalDays = totalDays;
            return this;
        }

        public Builder usedDays(int usedDays) {
            this.usedDays = usedDays;
            return this;
        }

        public Builder remainingDays(int remainingDays) {
            this.remainingDays = remainingDays;
            return this;
        }

        public LeaveBalanceResponse build() {
            return new LeaveBalanceResponse(id, employeeId, employeeName, year, leaveType, totalDays, usedDays, remainingDays);
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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(int usedDays) {
        this.usedDays = usedDays;
    }

    public int getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(int remainingDays) {
        this.remainingDays = remainingDays;
    }
}
