package com.dtao.lms.utils;

import com.dtao.lms.dto.ExamSubmitRequest;
import com.dtao.lms.model.AnswerRecord;
import com.dtao.lms.model.Question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ ExamEvaluatorUtil
 * Final version — correctly matches questions by ID and shows correct answers,
 * student answers, question text, and explanation.
 */
public class ExamEvaluatorUtil {

    /**
     * Evaluate student answers and generate full answer records.
     *
     * @param questions The list of exam questions (from DB)
     * @param answers   The student's submitted answers (from frontend)
     * @return EvaluationResult containing question-wise evaluation
     */
    public static EvaluationResult evaluateAnswers(List<Question> questions, List<ExamSubmitRequest.Answer> answers) {
        List<AnswerRecord> answerRecords = new ArrayList<>();
        int correctCount = 0;

        // ✅ Map student's answers by question ID for faster lookup
        Map<String, String> answerMap = new HashMap<>();
        for (ExamSubmitRequest.Answer ans : answers) {
            if (ans.getQuestionId() != null && ans.getSelectedOption() != null) {
                answerMap.put(ans.getQuestionId().trim(), ans.getSelectedOption().trim());
            }
        }

        // ✅ Loop through questions and evaluate
        for (Question q : questions) {
            String selectedRaw = answerMap.getOrDefault(q.getId(), null);
            String correctRaw = q.getAnswer();

            // Normalize both for consistent comparison
            String selected = normalize(selectedRaw);
            String correctOpt = normalize(correctRaw);

            boolean isCorrect = selected.equalsIgnoreCase(correctOpt);
            if (isCorrect) correctCount++;

            // ✅ Build answer record with correct details
            answerRecords.add(new AnswerRecord(
                    q.getId(),
                    q.getQuestion() != null ? q.getQuestion() : "Question text missing",
                    selectedRaw != null ? selectedRaw : "Not answered",
                    correctRaw != null ? correctRaw : "N/A",
                    isCorrect,
                    q.getExplanation() != null ? q.getExplanation() : "No explanation available"
            ));
        }

        int total = questions.size();
        int wrongCount = total - correctCount;
        double percentage = total == 0 ? 0 : (correctCount * 100.0 / total);

        return new EvaluationResult(answerRecords, correctCount, wrongCount, total, percentage);
    }

    /**
     * ✅ Normalize input — handles cases like "OptionC" → "C"
     */
    private static String normalize(String input) {
        if (input == null) return "";
        input = input.trim();
        if (input.equalsIgnoreCase("OptionA")) return "A";
        if (input.equalsIgnoreCase("OptionB")) return "B";
        if (input.equalsIgnoreCase("OptionC")) return "C";
        if (input.equalsIgnoreCase("OptionD")) return "D";
        return input;
    }

    /**
     * ✅ Inner static class that holds the evaluation summary
     */
    public static class EvaluationResult {
        private final List<AnswerRecord> answerRecords;
        private final int correctCount;
        private final int wrongCount;
        private final int totalQuestions;
        private final double percentage;

        public EvaluationResult(List<AnswerRecord> answerRecords, int correctCount, int wrongCount, int totalQuestions, double percentage) {
            this.answerRecords = answerRecords;
            this.correctCount = correctCount;
            this.wrongCount = wrongCount;
            this.totalQuestions = totalQuestions;
            this.percentage = percentage;
        }

        public List<AnswerRecord> getAnswerRecords() {
            return answerRecords;
        }

        public int getCorrectCount() {
            return correctCount;
        }

        public int getWrongCount() {
            return wrongCount;
        }

        public int getTotalQuestions() {
            return totalQuestions;
        }

        public double getPercentage() {
            return percentage;
        }
    }
}
