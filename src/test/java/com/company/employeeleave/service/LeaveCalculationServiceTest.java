package com.company.employeeleave.service;

import com.company.employeeleave.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaveCalculationServiceTest {

    private LeaveCalculationService service;

    @BeforeEach
    void setUp() {
        service = new LeaveCalculationService();
    }

    @Test
    @DisplayName("Working days across standard weekday: Monday to Wednesday = 3 days")
    void testStandardWeekdays() {
        // Monday 2026-06-08 to Wednesday 2026-06-10
        LocalDate start = LocalDate.of(2026, 6, 8);
        LocalDate end = LocalDate.of(2026, 6, 10);

        int days = service.calculateWorkingDays(start, end);

        assertThat(days).isEqualTo(3);
    }

    @Test
    @DisplayName("Weekends must be excluded: Friday to Monday = 2 days (Sat/Sun excluded)")
    void testWeekendExclusion() {
        // Friday 2026-06-12 to Monday 2026-06-15
        LocalDate start = LocalDate.of(2026, 6, 12);
        LocalDate end = LocalDate.of(2026, 6, 15);

        int days = service.calculateWorkingDays(start, end);

        assertThat(days).isEqualTo(2); // Friday and Monday only
    }

    @Test
    @DisplayName("Public holiday exclusion: 2026-12-24 to 2026-12-28 excludes Christmas and weekend")
    void testPublicHolidayExclusion() {
        // 2026-12-24 (Thursday) to 2026-12-28 (Monday)
        // 12-24: Thursday (workday) -> 1
        // 12-25: Friday (Christmas Day holiday) -> 0
        // 12-26: Saturday (weekend) -> 0
        // 12-27: Sunday (weekend) -> 0
        // 12-28: Monday (workday) -> 1
        // Total = 2 working days
        LocalDate start = LocalDate.of(2026, 12, 24);
        LocalDate end = LocalDate.of(2026, 12, 28);

        int days = service.calculateWorkingDays(start, end);

        assertThat(days).isEqualTo(2);
    }

    @Test
    @DisplayName("Invalid date range where start date is after end date throws BusinessRuleException")
    void testInvalidDateRange() {
        LocalDate start = LocalDate.of(2026, 6, 15);
        LocalDate end = LocalDate.of(2026, 6, 10);

        assertThatThrownBy(() -> service.calculateWorkingDays(start, end))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("start date");
    }

    @Test
    @DisplayName("Zero working days range (e.g. only Saturday and Sunday) throws BusinessRuleException")
    void testZeroWorkingDays() {
        // Saturday 2026-06-13 to Sunday 2026-06-14
        LocalDate start = LocalDate.of(2026, 6, 13);
        LocalDate end = LocalDate.of(2026, 6, 14);

        assertThatThrownBy(() -> service.calculateWorkingDays(start, end))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("0 working days");
    }
}
