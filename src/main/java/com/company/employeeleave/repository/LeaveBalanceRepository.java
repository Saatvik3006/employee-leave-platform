package com.company.employeeleave.repository;

import com.company.employeeleave.domain.entity.LeaveBalance;
import com.company.employeeleave.domain.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndYearAndLeaveType(Long employeeId, int year, LeaveType leaveType);

    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);

    List<LeaveBalance> findByEmployeeId(Long employeeId);

    @Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.id = :employeeId AND lb.year = :year AND lb.leaveType = :leaveType")
    Optional<LeaveBalance> findBalanceForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("year") int year,
            @Param("leaveType") LeaveType leaveType
    );
}
