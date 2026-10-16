package com.company.employeeleave.service;

import com.company.employeeleave.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Service responsible for calculating working days for leave requests.
 * Business Rules:
 * 1. Weekends (Saturdays and Sundays) are strictly excluded from leave duration.
 * 2. Official company fixed public holidays are excluded from leave duration.
 * 3. Start date must be on or before end date.
 * 4. Duration must result in at least one valid working day.
 */
@Service
public class LeaveCalculationService {

    // Standard recognized company holidays (fixed month-day)
    private static final Set<MonthDay> FIXED_COMPANY_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),   // New Year's Day
            MonthDay.of(1, 26),  // Republic Day / National Holiday
            MonthDay.of(5, 1),   // Labour Day
            MonthDay.of(8, 15),  // Independence Day
            MonthDay.of(10, 2),  // National Day
            MonthDay.of(12, 25)  // Christmas Day
    );

    public int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("Start date and end date must not be null.");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                    String.format("Invalid date range: start date (%s) must be before or equal to end date (%s).",
                            startDate, endDate)
            );
        }

        int workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (isWorkingDay(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        if (workingDays == 0) {
            throw new BusinessRuleException(
                    String.format("The requested leave range (%s to %s) contains only weekends or public holidays (0 working days).",
                            startDate, endDate)
            );
        }

        return workingDays;
    }

    public boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !isCompanyHoliday(date);
    }

    public boolean isCompanyHoliday(LocalDate date) {
        MonthDay monthDay = MonthDay.of(date.getMonth(), date.getDayOfMonth());
        return FIXED_COMPANY_HOLIDAYS.contains(monthDay);
    }
}
