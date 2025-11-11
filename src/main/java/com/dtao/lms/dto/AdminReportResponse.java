package com.dtao.lms.dto;

import com.dtao.lms.model.Report;

import java.util.List;

public class AdminReportResponse {
    private int page;
    private int size;
    private long total;
    private List<Report> reports;

    public AdminReportResponse() {}
    public AdminReportResponse(int page, int size, long total, List<Report> reports) {
        this.page = page; this.size = size; this.total = total; this.reports = reports;
    }

    // getters / setters
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public List<Report> getReports() { return reports; }
    public void setReports(List<Report> reports) { this.reports = reports; }
}
