package com.dtao.lms.utils;

import com.dtao.lms.model.Question;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVParserUtil {

    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
            "Question", "OptionA", "OptionB", "OptionC", "OptionD", "Answer", "Explanation"
    );

    public static List<Question> parseAndValidateCSV(MultipartFile file) {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            List<String> headers = Arrays.asList(headerLine.split(","));
            validateHeaders(headers);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length < 7) continue;

                Question q = new Question(
                        data[0].trim(),
                        data[1].trim(),
                        data[2].trim(),
                        data[3].trim(),
                        data[4].trim(),
                        data[5].trim(),
                        data[6].trim()
                );
                questions.add(q);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error parsing CSV: " + e.getMessage());
        }

        return questions;
    }

    private static void validateHeaders(List<String> headers) {
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (!headers.get(i).trim().equalsIgnoreCase(EXPECTED_HEADERS.get(i))) {
                throw new RuntimeException("Invalid CSV header format. Expected: " + EXPECTED_HEADERS);
            }
        }
    }
}
