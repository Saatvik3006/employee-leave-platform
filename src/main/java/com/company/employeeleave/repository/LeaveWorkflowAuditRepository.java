package com.company.employeeleave.repository;

import com.company.employeeleave.domain.entity.LeaveWorkflowAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveWorkflowAuditRepository extends JpaRepository<LeaveWorkflowAudit, Long> {

    List<LeaveWorkflowAudit> findByLeaveRequestIdOrderByActionTimestampDesc(Long leaveRequestId);
}
