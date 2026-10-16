package com.company.employeeleave.domain.entity;

import com.company.employeeleave.domain.enums.LeaveType;
import com.company.employeeleave.exception.InsufficientLeaveBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "leave_balances",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_emp_year_type", columnNames = {"employee_id", "calendar_year", "leave_type"})
    }
)
public class LeaveBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "calendar_year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 50)
    private LeaveType leaveType;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "used_days", nullable = false)
    private int usedDays = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public LeaveBalance() {
    }

    public LeaveBalance(Long id, Employee employee, int year, LeaveType leaveType, int totalDays, int usedDays, Long version) {
        this.id = id;
        this.employee = employee;
        this.year = year;
        this.leaveType = leaveType;
        this.totalDays = totalDays;
        this.usedDays = usedDays;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Employee employee;
        private int year;
        private LeaveType leaveType;
        private int totalDays;
        private int usedDays = 0;
        private Long version;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder employee(Employee employee) {
            this.employee = employee;
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

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public LeaveBalance build() {
            return new LeaveBalance(id, employee, year, leaveType, totalDays, usedDays, version);
        }
    }

    public int getRemainingDays() {
        return totalDays - usedDays;
    }

    public boolean hasSufficientBalance(int requestedDays) {
        return getRemainingDays() >= requestedDays;
    }

    public void deduct(int days) {
        if (!hasSufficientBalance(days)) {
            throw new InsufficientLeaveBalanceException(
                String.format("Insufficient leave balance for %s: requested %d days, but only %d days remaining.",
                    leaveType, days, getRemainingDays())
            );
        }
        this.usedDays += days;
    }

    public void restore(int days) {
        if (this.usedDays - days < 0) {
            this.usedDays = 0;
        } else {
            this.usedDays -= days;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
