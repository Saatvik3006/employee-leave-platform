package com.company.employeeleave.dto;

import jakarta.validation.constraints.Size;

public class LeaveDecisionRequest {

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String comments;

    public LeaveDecisionRequest() {
    }

    public LeaveDecisionRequest(String comments) {
        this.comments = comments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String comments;

        public Builder comments(String comments) {
            this.comments = comments;
            return this;
        }

        public LeaveDecisionRequest build() {
            return new LeaveDecisionRequest(comments);
        }
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
