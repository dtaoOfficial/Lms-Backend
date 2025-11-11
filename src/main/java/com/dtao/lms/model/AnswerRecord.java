package com.dtao.lms.model;

/**
 * ✅ AnswerRecord
 * Embedded subdocument representing a student's response
 * to one question during an exam attempt.
 */
public class AnswerRecord {

    private String questionId;     // Reference to Question._id
    private String questionText;   // For result review
    private String selectedOption; // What the student selected
    private String correctAnswer;  // ✅ renamed for frontend consistency
    private boolean correct;       // true if student is correct
    private String explanation;    // Optional explanation for review

    // 🧱 No-Args Constructor
    public AnswerRecord() {}

    // 🧱 All-Args Constructor
    public AnswerRecord(String questionId, String questionText, String selectedOption,
                        String correctAnswer, boolean correct, String explanation) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.selectedOption = selectedOption;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
        this.explanation = explanation;
    }

    // 🧩 Getters and Setters
    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

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

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
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

    // 🧭 toString() for debugging/logging
    @Override
    public String toString() {
        return "AnswerRecord{" +
                "questionId='" + questionId + '\'' +
                ", questionText='" + questionText + '\'' +
                ", selectedOption='" + selectedOption + '\'' +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", correct=" + correct +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
