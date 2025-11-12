package com.dtao.lms.service;

import com.dtao.lms.model.Exam;
import com.dtao.lms.model.Question;
import com.dtao.lms.repo.ExamRepository;
import com.dtao.lms.utils.CSVParserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ✅ ExamCSVService (Final Production Version)
 * Handles reliable CSV uploads for exam questions.
 * Supports:
 * - Questions with commas, quotes, %, or semicolons
 * - Header validation
 * - UTF-8 BOM-safe parsing
 * - Detailed error logs
 */
@Service
public class ExamCSVService {

    private final ExamRepository examRepository;

    @Autowired
    public ExamCSVService(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    /**
     * ✅ Uploads and parses a CSV, then attaches parsed questions to an existing exam.
     */
    public Exam uploadQuestionsFromCSV(String examId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("❌ No file uploaded. Please select a CSV file before proceeding.");
        }

        // ✅ Step 1: Validate Exam
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("❌ Exam not found with ID: " + examId));

        try {
            // ✅ Step 2: Parse CSV safely
            List<Question> questions = CSVParserUtil.parseAndValidateCSV(file);

            if (questions == null || questions.isEmpty()) {
                throw new RuntimeException("⚠️ No valid questions found in the uploaded CSV file.");
            }

            // ✅ Step 3: Attach to Exam and Save
            exam.setQuestions(questions);
            Exam savedExam = examRepository.save(exam);

            System.out.println(String.format(
                    "✅ Successfully imported %d questions into exam '%s' (ID: %s)",
                    questions.size(),
                    exam.getName(),
                    exam.getId()
            ));

            return savedExam;

        } catch (RuntimeException e) {
            // Rethrow with more clarity for controller
            throw new RuntimeException("❌ CSV upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("💥 Unexpected error while processing CSV: " + e.getMessage(), e);
        }
    }
}
