package com.company.employeeleave.repository;

import com.company.employeeleave.domain.entity.LeaveRequest;
import com.company.employeeleave.domain.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.manager", "decidedBy"})
    Optional<LeaveRequest> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"employee", "employee.department", "decidedBy"})
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "employee.department", "decidedBy"})
    Page<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status, Pageable pageable);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "JOIN FETCH lr.employee e " +
           "LEFT JOIN FETCH e.department " +
           "LEFT JOIN FETCH lr.decidedBy " +
           "WHERE e.manager.id = :managerId AND (:status IS NULL OR lr.status = :status)")
    Page<LeaveRequest> findByManagerScope(
            @Param("managerId") Long managerId,
            @Param("status") LeaveStatus status,
            Pageable pageable
    );

    @Query("SELECT lr FROM LeaveRequest lr " +
           "JOIN FETCH lr.employee e " +
           "LEFT JOIN FETCH e.department " +
           "LEFT JOIN FETCH lr.decidedBy " +
           "WHERE (:status IS NULL OR lr.status = :status)")
    Page<LeaveRequest> findAllWithDetails(
            @Param("status") LeaveStatus status,
            Pageable pageable
    );

    @Query("SELECT COUNT(lr) > 0 FROM LeaveRequest lr " +
           "WHERE lr.employee.id = :employeeId " +
           "AND lr.status IN ('PENDING', 'APPROVED') " +
           "AND lr.startDate <= :endDate " +
           "AND lr.endDate >= :startDate " +
           "AND (:excludeId IS NULL OR lr.id != :excludeId)")
    boolean existsOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );
}
