package com.company.employeeleave.repository;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.domain.entity.Employee;
import com.company.employeeleave.domain.enums.EmployeeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department savedDept;

    @BeforeEach
    void setUp() {
        savedDept = departmentRepository.save(
                Department.builder().code("FIN").name("Finance").active(true).build()
        );
    }

    @Test
    @DisplayName("Unique email constraint violation throws DataIntegrityViolationException")
    void testUniqueEmailConstraint() {
        Employee emp1 = Employee.builder()
                .employeeCode("EMP-101")
                .firstName("John")
                .lastName("Doe")
                .email("duplicate@company.com")
                .jobTitle("Accountant")
                .department(savedDept)
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();
        employeeRepository.saveAndFlush(emp1);

        Employee emp2 = Employee.builder()
                .employeeCode("EMP-102")
                .firstName("Jane")
                .lastName("Smith")
                .email("duplicate@company.com") // Duplicate!
                .jobTitle("Financial Analyst")
                .department(savedDept)
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();

        assertThatThrownBy(() -> employeeRepository.saveAndFlush(emp2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Unique employeeCode constraint violation throws DataIntegrityViolationException")
    void testUniqueEmployeeCodeConstraint() {
        Employee emp1 = Employee.builder()
                .employeeCode("EMP-CODE-DUP")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@company.com")
                .jobTitle("Accountant")
                .department(savedDept)
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();
        employeeRepository.saveAndFlush(emp1);

        Employee emp2 = Employee.builder()
                .employeeCode("EMP-CODE-DUP") // Duplicate code!
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@company.com")
                .jobTitle("Financial Analyst")
                .department(savedDept)
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();

        assertThatThrownBy(() -> employeeRepository.saveAndFlush(emp2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findWithDetailsById fetches employee with department eagerly")
    void testFindWithDetailsById() {
        Employee emp = Employee.builder()
                .employeeCode("EMP-103")
                .firstName("Robert")
                .lastName("Fox")
                .email("robert.fox@company.com")
                .jobTitle("Treasury Manager")
                .department(savedDept)
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();
        Employee saved = employeeRepository.saveAndFlush(emp);

        Employee found = employeeRepository.findWithDetailsById(saved.getId()).orElseThrow();

        assertThat(found.getDepartment()).isNotNull();
        assertThat(found.getDepartment().getName()).isEqualTo("Finance");
    }
}
