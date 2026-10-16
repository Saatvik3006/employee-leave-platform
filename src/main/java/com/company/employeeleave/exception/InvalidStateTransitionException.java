package com.company.employeeleave.exception;

public class InvalidStateTransitionException extends BusinessRuleException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
