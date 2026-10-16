package com.company.employeeleave.exception;

public class InsufficientLeaveBalanceException extends BusinessRuleException {
    public InsufficientLeaveBalanceException(String message) {
        super(message);
    }
}
