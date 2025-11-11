package com.dtao.lms.utils;

import com.dtao.lms.model.Exam;

import java.time.LocalDateTime;

public class ExamValidator {

    public static void validateExamDates(Exam exam) {
        if (exam.getStartDate() == null || exam.getEndDate() == null) {
            throw new RuntimeException("Start and End dates are required.");
        }
        if (exam.getEndDate().isBefore(exam.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date.");
        }
    }

    public static void validateExamDuration(Exam exam) {
        if (exam.getDuration() <= 0) {
            throw new RuntimeException("Exam duration must be greater than 0 minutes.");
        }
    }

    public static void validateExamVisibility(Exam exam) {
        if (!exam.isPublished() && LocalDateTime.now().isAfter(exam.getStartDate())) {
            throw new RuntimeException("Cannot unpublish an exam that has already started.");
        }
    }
}
