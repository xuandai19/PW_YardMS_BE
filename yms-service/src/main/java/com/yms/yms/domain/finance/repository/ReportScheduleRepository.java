package com.yms.yms.domain.finance.repository;

import com.yms.yms.domain.finance.entity.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {
    List<ReportSchedule> findByActiveTrueAndReportType(String reportType);
}
