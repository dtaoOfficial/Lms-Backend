package com.dtao.lms.controller;

import com.dtao.lms.dto.AdminReportResponse;
import com.dtao.lms.model.Report;
import com.dtao.lms.service.AdminReportService;
import com.dtao.lms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;
    private final AdminReportService adminReportService;

    @Autowired
    public AdminReportController(ReportService reportService, AdminReportService adminReportService) {
        this.reportService = reportService;
        this.adminReportService = adminReportService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listReports(@RequestParam(required = false) String status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        List<Report> list = reportService.listReports(status, page, size);
        long total = list.size();
        return ResponseEntity.ok(new AdminReportResponse(page, size, total, list));
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateReport(@PathVariable String reportId,
                                          @RequestParam String status,
                                          @RequestParam(required = false) String note,
                                          Authentication auth) {
        String adminEmail = auth.getName();
        Optional<Report> updated = reportService.updateReportStatus(reportId, adminEmail, status.toUpperCase(), note);
        return updated.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ NEW: delete a report entirely
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteReport(@PathVariable String reportId) {
        boolean deleted = adminReportService.deleteReport(reportId);
        if (deleted) return ResponseEntity.ok().body("Report deleted successfully");
        return ResponseEntity.notFound().build();
    }

    // ✅ NEW: get single report detail
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getReportDetail(@PathVariable String reportId) {
        Optional<Report> report = adminReportService.getReportById(reportId);
        return report.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
