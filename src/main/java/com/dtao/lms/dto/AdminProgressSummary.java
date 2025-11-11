package com.dtao.lms.dto;

import java.util.List;

public class AdminProgressSummary {

    private int totalStudents;
    private double averageProgress;
    private List<TopStudent> topStudents;
    private List<CourseAvg> perCourse;

    // Getters & Setters
    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public double getAverageProgress() {
        return averageProgress;
    }

    public void setAverageProgress(double averageProgress) {
        this.averageProgress = averageProgress;
    }

    public List<TopStudent> getTopStudents() {
        return topStudents;
    }

    public void setTopStudents(List<TopStudent> topStudents) {
        this.topStudents = topStudents;
    }

    public List<CourseAvg> getPerCourse() {
        return perCourse;
    }

    public void setPerCourse(List<CourseAvg> perCourse) {
        this.perCourse = perCourse;
    }

    // --- Inner DTOs ---
    public static class TopStudent {
        private String name;
        private String email;
        private double progress;

        public TopStudent() {}
        public TopStudent(String name, String email, double progress) {
            this.name = name;
            this.email = email;
            this.progress = progress;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public double getProgress() { return progress; }
        public void setName(String name) { this.name = name; }
        public void setEmail(String email) { this.email = email; }
        public void setProgress(double progress) { this.progress = progress; }
    }

    public static class CourseAvg {
        private String courseTitle;
        private double avgCompletion;

        public CourseAvg() {}
        public CourseAvg(String courseTitle, double avgCompletion) {
            this.courseTitle = courseTitle;
            this.avgCompletion = avgCompletion;
        }

        public String getCourseTitle() { return courseTitle; }
        public double getAvgCompletion() { return avgCompletion; }
        public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
        public void setAvgCompletion(double avgCompletion) { this.avgCompletion = avgCompletion; }
    }
}
