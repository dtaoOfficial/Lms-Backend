package com.dtao.lms.utils;

import com.dtao.lms.model.Question;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ✅ CSVParserUtil (Final Fault-Tolerant Version)
 * Handles CSVs with:
 *  - Custom delimiters (¬)
 *  - Unique quoteChar (using £)
 *  - Supports commas, quotes, pipes, ||, <, > safely
 *  - Removes broken newlines and trims whitespace
 *  - UTF-8 safe (Excel/Sheets compatible)
 */
public class CSVParserUtil {

    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
            "Question", "OptionA", "OptionB", "OptionC", "OptionD", "Answer", "Explanation"
    );

    public static List<Question> parseAndValidateCSV(MultipartFile file) {
        List<Question> questions = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {

            // ✅ Configured for your custom format
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator('¬')        // custom column separator
                    .withQuoteChar('£')        // use £ to handle commas safely
                    .withEscapeChar((char) 0)  // disable escape parsing
                    .withIgnoreLeadingWhiteSpace(true)
                    .withStrictQuotes(false)
                    .build();

            CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();

            List<String[]> allRows = new ArrayList<>();

            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                // Join properly with custom delimiter (NOT comma)
                String joined = String.join("¬", nextLine)
                        .replaceAll("[\r\n]+", " ") // remove extra newlines
                        .replaceAll("\\s+", " ")     // normalize spaces
                        .trim();

                if (joined.isEmpty()) continue;

                // Split safely using our safeSplit method
                String[] cleaned = safeSplit(joined);
                allRows.add(cleaned);
            }

            if (allRows.isEmpty()) {
                throw new RuntimeException("❌ CSV file is empty.");
            }

            // ✅ Validate header row
            String[] headerRow = allRows.get(0);
            validateHeaders(Arrays.asList(headerRow));

            // ✅ Process and build Question objects
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                if (row == null || row.length < EXPECTED_HEADERS.size()) continue;

                for (int j = 0; j < row.length; j++) {
                    row[j] = safeTrim(row[j]);
                }

                Question q = new Question(
                        row[0], row[1], row[2], row[3], row[4], row[5], row[6]
                );

                questions.add(q);
            }

        } catch (Exception e) {
            throw new RuntimeException("💥 Error parsing CSV file: " + e.getMessage(), e);
        }

        return questions;
    }

    /**
     * 🧠 Safe split using custom delimiter (¬)
     * Handles embedded commas, quotes, and symbols safely.
     */
    private static String[] safeSplit(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // Accept both £ and " as quote characters (OpenCSV may strip one)
            if (c == '£' || c == '"') {
                inQuotes = !inQuotes;
            } else if (c == '¬' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    /**
     * 🧹 Trim and clean quotes safely.
     */
    private static String safeTrim(String text) {
        return text == null ? "" : text.trim().replaceAll("^£|£$", "");
    }

    /**
     * 🧾 Validate CSV headers strictly but case-insensitive.
     */
    private static void validateHeaders(List<String> headers) {
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (i >= headers.size()) {
                throw new RuntimeException("❌ Missing header column: " + EXPECTED_HEADERS.get(i));
            }

            String actual = headers.get(i)
                    .replace("\uFEFF", "")
                    .replaceAll("£", "")
                    .trim()
                    .toLowerCase();
            String expected = EXPECTED_HEADERS.get(i).toLowerCase();

            if (!actual.equals(expected)) {
                throw new RuntimeException("❌ Invalid header '" + actual
                        + "' (expected '" + expected + "')");
            }
        }
    }
}
