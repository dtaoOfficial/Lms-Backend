package com.dtao.lms.service;

import com.dtao.lms.model.Exam;
import com.dtao.lms.model.Question;
import com.dtao.lms.repo.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ExamCSVService {

    private final ExamRepository examRepository;

    @Autowired
    public ExamCSVService(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    /**
     * Upload and parse CSV file for MCQ exams.
     * Validates header structure and maps to Question objects.
     */
    public Exam uploadQuestionsFromCSV(String examId, MultipartFile file) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));

        List<Question> questions = parseCSV(file);
        exam.setQuestions(questions);

        return examRepository.save(exam);
    }

    /**
     * Parse and validate CSV headers + content
     */
    private List<Question> parseCSV(MultipartFile file) {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            List<String> headers = Arrays.asList(headerLine.split(","));
            validateCSVHeaders(headers);

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                if (data.length < 7) continue; // skip invalid rows

                Question question = new Question(
                        data[0].trim(), // question
                        data[1].trim(), // optionA
                        data[2].trim(), // optionB
                        data[3].trim(), // optionC
                        data[4].trim(), // optionD
                        data[5].trim(), // answer
                        data[6].trim()  // explanation
                );

                questions.add(question);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }

        return questions;
    }

    /**
     * Validate the CSV header structure
     */
    private void validateCSVHeaders(List<String> headers) {
        List<String> expected = Arrays.asList(
                "Question", "OptionA", "OptionB", "OptionC", "OptionD", "Answer", "Explanation"
        );

        for (int i = 0; i < expected.size(); i++) {
            if (!headers.get(i).trim().equalsIgnoreCase(expected.get(i))) {
                throw new RuntimeException("Invalid CSV format. Expected header: " + expected);
            }
        }
    }
}
