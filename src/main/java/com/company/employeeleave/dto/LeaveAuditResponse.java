package com.company.employeeleave.dto;

import com.company.employeeleave.domain.enums.LeaveStatus;

import java.time.LocalDateTime;

public class LeaveAuditResponse {

    private Long id;
    private Long leaveRequestId;
    private LeaveStatus previousStatus;
    private LeaveStatus newStatus;
    private String actionBy;
    private LocalDateTime actionTimestamp;
    private String comments;

    public LeaveAuditResponse() {
    }

    public LeaveAuditResponse(Long id, Long leaveRequestId, LeaveStatus previousStatus,
                              LeaveStatus newStatus, String actionBy, LocalDateTime actionTimestamp, String comments) {
        this.id = id;
        this.leaveRequestId = leaveRequestId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actionBy = actionBy;
        this.actionTimestamp = actionTimestamp;
        this.comments = comments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long leaveRequestId;
        private LeaveStatus previousStatus;
        private LeaveStatus newStatus;
        private String actionBy;
        private LocalDateTime actionTimestamp;
        private String comments;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder leaveRequestId(Long leaveRequestId) {
            this.leaveRequestId = leaveRequestId;
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

        public LeaveAuditResponse build() {
            return new LeaveAuditResponse(id, leaveRequestId, previousStatus, newStatus, actionBy, actionTimestamp, comments);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLeaveRequestId() {
        return leaveRequestId;
    }

    public void setLeaveRequestId(Long leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
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
