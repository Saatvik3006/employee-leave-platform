package com.company.employeeleave.service;

import com.company.employeeleave.domain.entity.Department;
import com.company.employeeleave.dto.DepartmentRequest;
import com.company.employeeleave.dto.DepartmentResponse;
import com.company.employeeleave.dto.PagedResponse;
import com.company.employeeleave.exception.DuplicateResourceException;
import com.company.employeeleave.exception.ResourceNotFoundException;
import com.company.employeeleave.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department with code '" + request.getCode() + "' already exists.");
        }

        Department department = Department.builder()
                .code(request.getCode().toUpperCase().trim())
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(request.isActive())
                .build();

        return mapToResponse(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public PagedResponse<DepartmentResponse> getAllDepartments(Pageable pageable) {
        Page<DepartmentResponse> page = departmentRepository.findAll(pageable)
                .map(this::mapToResponse);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartments() {
        return departmentRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        if (!department.getCode().equalsIgnoreCase(request.getCode()) &&
                departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department with code '" + request.getCode() + "' already exists.");
        }

        department.setCode(request.getCode().toUpperCase().trim());
        department.setName(request.getName().trim());
        department.setDescription(request.getDescription());
        department.setActive(request.isActive());

        return mapToResponse(departmentRepository.save(department));
    }

    @Transactional
    public void deactivateDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        department.setActive(false);
        departmentRepository.save(department);
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .active(department.isActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
