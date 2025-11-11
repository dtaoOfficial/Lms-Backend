package com.dtao.lms.dto;

import java.util.List;

/**
 * ✅ Student's submission payload
 * Contains list of answered questions
 */
public class ExamSubmitRequest {

    private List<Answer> answers;

    public ExamSubmitRequest() {}
    public ExamSubmitRequest(List<Answer> answers) { this.answers = answers; }

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    @Override
    public String toString() {
        return "ExamSubmitRequest{" +
                "answers=" + answers +
                '}';
    }

    // Builder for ExamSubmitRequest
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private List<Answer> answers;
        public Builder answers(List<Answer> answers) { this.answers = answers; return this; }
        public ExamSubmitRequest build() { return new ExamSubmitRequest(answers); }
    }

    // 🧩 Inner static class Answer
    public static class Answer {
        private String questionId;
        private String selectedOption;

        public Answer() {}
        public Answer(String questionId, String selectedOption) {
            this.questionId = questionId;
            this.selectedOption = selectedOption;
        }

        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public String getSelectedOption() { return selectedOption; }
        public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }

        @Override
        public String toString() {
            return "Answer{" +
                    "questionId='" + questionId + '\'' +
                    ", selectedOption='" + selectedOption + '\'' +
                    '}';
        }

        // Manual Builder
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private String questionId;
            private String selectedOption;
            public Builder questionId(String questionId) { this.questionId = questionId; return this; }
            public Builder selectedOption(String selectedOption) { this.selectedOption = selectedOption; return this; }
            public Answer build() { return new Answer(questionId, selectedOption); }
        }
    }
}
