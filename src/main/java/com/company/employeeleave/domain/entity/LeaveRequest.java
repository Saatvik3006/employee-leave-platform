package com.company.employeeleave.domain.entity;

import com.company.employeeleave.domain.enums.LeaveStatus;
import com.company.employeeleave.domain.enums.LeaveType;
import com.company.employeeleave.exception.InvalidStateTransitionException;
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
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 50)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private Employee decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public LeaveRequest() {
    }

    public LeaveRequest(Long id, Employee employee, LeaveType leaveType, LocalDate startDate,
                        LocalDate endDate, int durationDays, String reason, LeaveStatus status,
                        String decisionReason, Employee decidedBy, LocalDateTime decidedAt, Long version) {
        this.id = id;
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.durationDays = durationDays;
        this.reason = reason;
        this.status = status != null ? status : LeaveStatus.PENDING;
        this.decisionReason = decisionReason;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Employee employee;
        private LeaveType leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private int durationDays;
        private String reason;
        private LeaveStatus status = LeaveStatus.PENDING;
        private String decisionReason;
        private Employee decidedBy;
        private LocalDateTime decidedAt;
        private Long version;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder employee(Employee employee) {
            this.employee = employee;
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

        public Builder decidedBy(Employee decidedBy) {
            this.decidedBy = decidedBy;
            return this;
        }

        public Builder decidedAt(LocalDateTime decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public LeaveRequest build() {
            return new LeaveRequest(id, employee, leaveType, startDate, endDate, durationDays,
                    reason, status, decisionReason, decidedBy, decidedAt, version);
        }
    }

    public void approve(Employee manager, String comments) {
        if (this.status != LeaveStatus.PENDING) {
            throw new InvalidStateTransitionException(
                String.format("Cannot approve leave request #%d: current status is %s (only PENDING requests can be approved).",
                    this.id, this.status)
            );
        }
        this.status = LeaveStatus.APPROVED;
        this.decidedBy = manager;
        this.decisionReason = comments;
        this.decidedAt = LocalDateTime.now();
    }

    public void reject(Employee manager, String reason) {
        if (this.status != LeaveStatus.PENDING) {
            throw new InvalidStateTransitionException(
                String.format("Cannot reject leave request #%d: current status is %s (only PENDING requests can be rejected).",
                    this.id, this.status)
            );
        }
        this.status = LeaveStatus.REJECTED;
        this.decidedBy = manager;
        this.decisionReason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (this.status == LeaveStatus.CANCELLED || this.status == LeaveStatus.REJECTED) {
            throw new InvalidStateTransitionException(
                String.format("Cannot cancel leave request #%d: request is already %s.", this.id, this.status)
            );
        }
        if (this.status == LeaveStatus.APPROVED && !this.startDate.isAfter(LocalDate.now())) {
            throw new InvalidStateTransitionException(
                String.format("Cannot cancel approved leave request #%d: start date (%s) is in the past or today.",
                    this.id, this.startDate)
            );
        }
        this.status = LeaveStatus.CANCELLED;
        this.decisionReason = reason != null ? reason : "Cancelled by requester";
        this.decidedAt = LocalDateTime.now();
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

    public Employee getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Employee decidedBy) {
        this.decidedBy = decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
