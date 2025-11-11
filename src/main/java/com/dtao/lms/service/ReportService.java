package com.dtao.lms.service;

import com.dtao.lms.model.Report;
import com.dtao.lms.model.TargetType;
import com.dtao.lms.repo.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    @Autowired
    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public Report createReport(TargetType type, String targetId, String email, String reason, String text) {
        if (reason == null || reason.trim().isEmpty()) reason = "OTHER";
        String txt = text == null ? "" : text.trim();
        if (txt.length() > 2000) throw new IllegalArgumentException("text too long");
        Report r = new Report(type, targetId, email, reason.trim(), txt);
        return reportRepository.save(r);
    }

    public List<Report> listReports(String status, int page, int size) {
        PageRequest p = PageRequest.of(Math.max(0,page), Math.max(1,size));
        if (status == null || status.trim().isEmpty()) {
            // return OPEN by default
            return reportRepository.findByStatusOrderByCreatedAtDesc("OPEN", p);
        }
        return reportRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), p);
    }

    public Optional<Report> updateReportStatus(String reportId, String adminEmail, String status, String adminNote) {
        Optional<Report> opt = reportRepository.findById(reportId);
        if (!opt.isPresent()) return Optional.empty();
        Report r = opt.get();
        r.setStatus(status);
        r.setHandledBy(adminEmail);
        r.setHandledAt(Instant.now());
        r.setAdminNote(adminNote);
        reportRepository.save(r);
        return Optional.of(r);
    }

    // ✅ NEW: Delete a report by ID (for admin cleanup)
    public void deleteReportById(String id) {
        try {
            reportRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete report: " + e.getMessage());
        }
    }
    // add this helper method near bottom of ReportService.java

    /**
     * Retrieve a specific report by ID (read-only helper for admin view)
     */
    public Optional<Report> getReportById(String id) {
        return reportRepository.findById(id);
    }

}
