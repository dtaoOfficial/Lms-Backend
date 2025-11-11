package com.dtao.lms.service;

import com.dtao.lms.model.Report;
import com.dtao.lms.repo.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminReportService {

    private final ReportRepository reportRepository;

    @Autowired
    public AdminReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Fetch all reports filtered by status.
     * If status is null → fetch OPEN reports by default.
     */
    public List<Report> getReports(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        String queryStatus = (status == null || status.isBlank()) ? "OPEN" : status.toUpperCase();
        return reportRepository.findByStatusOrderByCreatedAtDesc(queryStatus, pageable);
    }

    /**
     * Delete a report completely from DB (admin cleanup).
     */
    public boolean deleteReport(String id) {
        if (!reportRepository.existsById(id)) return false;
        reportRepository.deleteById(id);
        return true;
    }

    /**
     * Get total reports by a particular status (for dashboard counts).
     */
    public long countReportsByStatus(String status) {
        return reportRepository.countByStatus(status.toUpperCase());
    }

    /**
     * Fetch single report detail.
     */
    public Optional<Report> getReportById(String id) {
        return reportRepository.findById(id);
    }
}
