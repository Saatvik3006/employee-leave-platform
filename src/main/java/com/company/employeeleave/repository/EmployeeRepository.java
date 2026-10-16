package com.company.employeeleave.repository;

import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    @EntityGraph(attributePaths = {"department", "manager"})
    Optional<Employee> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"department", "manager"})
    Optional<Employee> findWithDetailsByEmail(String email);

    @EntityGraph(attributePaths = {"department", "manager"})
    Page<Employee> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"department", "manager"})
    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "manager"})
    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "manager"})
    List<Employee> findByManagerId(Long managerId);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN FETCH e.department " +
           "LEFT JOIN FETCH e.manager " +
           "WHERE (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:search IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Employee> searchEmployees(
            @Param("departmentId") Long departmentId,
            @Param("status") EmployeeStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
