package com.dtao.lms.dto;

/**
 * ✅ Represents a single question shown to the student during the exam
 * Only includes options, no answers
 */
public class ExamQuestionResponse {
    private String questionId;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    public ExamQuestionResponse() {}

    public ExamQuestionResponse(String questionId, String question, String optionA, String optionB, String optionC, String optionD) {
        this.questionId = questionId;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
    }

    // Getters and Setters
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    @Override
    public String toString() {
        return "ExamQuestionResponse{" +
                "questionId='" + questionId + '\'' +
                ", question='" + question + '\'' +
                ", optionA='" + optionA + '\'' +
                ", optionB='" + optionB + '\'' +
                ", optionC='" + optionC + '\'' +
                ", optionD='" + optionD + '\'' +
                '}';
    }

    // Manual Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String questionId;
        private String question;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;

        public Builder questionId(String questionId) { this.questionId = questionId; return this; }
        public Builder question(String question) { this.question = question; return this; }
        public Builder optionA(String optionA) { this.optionA = optionA; return this; }
        public Builder optionB(String optionB) { this.optionB = optionB; return this; }
        public Builder optionC(String optionC) { this.optionC = optionC; return this; }
        public Builder optionD(String optionD) { this.optionD = optionD; return this; }

        public ExamQuestionResponse build() {
            return new ExamQuestionResponse(questionId, question, optionA, optionB, optionC, optionD);
        }
    }
}
