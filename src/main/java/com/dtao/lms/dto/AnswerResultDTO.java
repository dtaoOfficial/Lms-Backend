package com.dtao.lms.dto;

/**
 * ✅ Used for each question in result page
 * Shows student's answer vs correct answer and explanation
 */
public class AnswerResultDTO {

    private String questionText;
    private String selectedOption;
    private String correctOption;
    private boolean correct;
    private String explanation;

    // 🧱 No-Args Constructor
    public AnswerResultDTO() {}

    // 🧱 All-Args Constructor
    public AnswerResultDTO(String questionText, String selectedOption, String correctOption, boolean correct, String explanation) {
        this.questionText = questionText;
        this.selectedOption = selectedOption;
        this.correctOption = correctOption;
        this.correct = correct;
        this.explanation = explanation;
    }

    // 🧩 Getters and Setters
    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    // 🧭 toString() for logging/debugging
    @Override
    public String toString() {
        return "AnswerResultDTO{" +
                "questionText='" + questionText + '\'' +
                ", selectedOption='" + selectedOption + '\'' +
                ", correctOption='" + correctOption + '\'' +
                ", correct=" + correct +
                ", explanation='" + explanation + '\'' +
                '}';
    }

    // 🧱 Manual Builder (replaces Lombok @Builder)
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String questionText;
        private String selectedOption;
        private String correctOption;
        private boolean correct;
        private String explanation;

        public Builder questionText(String questionText) {
            this.questionText = questionText;
            return this;
        }

        public Builder selectedOption(String selectedOption) {
            this.selectedOption = selectedOption;
            return this;
        }

        public Builder correctOption(String correctOption) {
            this.correctOption = correctOption;
            return this;
        }

        public Builder correct(boolean correct) {
            this.correct = correct;
            return this;
        }

        public Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public AnswerResultDTO build() {
            return new AnswerResultDTO(questionText, selectedOption, correctOption, correct, explanation);
        }
    }
}
