package com.company.employeeleave.domain.entity;

import com.company.employeeleave.domain.enums.LeaveStatus;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_workflow_audits")
public class LeaveWorkflowAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequest leaveRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 50)
    private LeaveStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private LeaveStatus newStatus;

    @Column(name = "action_by", nullable = false, length = 150)
    private String actionBy;

    @Column(name = "action_timestamp", nullable = false)
    private LocalDateTime actionTimestamp = LocalDateTime.now();

    @Column(name = "comments", length = 500)
    private String comments;

    public LeaveWorkflowAudit() {
    }

    public LeaveWorkflowAudit(Long id, LeaveRequest leaveRequest, LeaveStatus previousStatus,
                              LeaveStatus newStatus, String actionBy, LocalDateTime actionTimestamp, String comments) {
        this.id = id;
        this.leaveRequest = leaveRequest;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actionBy = actionBy;
        this.actionTimestamp = actionTimestamp != null ? actionTimestamp : LocalDateTime.now();
        this.comments = comments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private LeaveRequest leaveRequest;
        private LeaveStatus previousStatus;
        private LeaveStatus newStatus;
        private String actionBy;
        private LocalDateTime actionTimestamp = LocalDateTime.now();
        private String comments;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder leaveRequest(LeaveRequest leaveRequest) {
            this.leaveRequest = leaveRequest;
            return this;
        }

        public Builder previousStatus(LeaveStatus previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }

        public Builder newStatus(LeaveStatus newStatus) {
            this.newStatus = newStatus;
            return this;
        }

        public Builder actionBy(String actionBy) {
            this.actionBy = actionBy;
            return this;
        }

        public Builder actionTimestamp(LocalDateTime actionTimestamp) {
            this.actionTimestamp = actionTimestamp;
            return this;
        }

        public Builder comments(String comments) {
            this.comments = comments;
            return this;
        }

        public LeaveWorkflowAudit build() {
            return new LeaveWorkflowAudit(id, leaveRequest, previousStatus, newStatus, actionBy, actionTimestamp, comments);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LeaveRequest getLeaveRequest() {
        return leaveRequest;
    }

    public void setLeaveRequest(LeaveRequest leaveRequest) {
        this.leaveRequest = leaveRequest;
    }

    public LeaveStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(LeaveStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public LeaveStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(LeaveStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getActionBy() {
        return actionBy;
    }

    public void setActionBy(String actionBy) {
        this.actionBy = actionBy;
    }

    public LocalDateTime getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(LocalDateTime actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
