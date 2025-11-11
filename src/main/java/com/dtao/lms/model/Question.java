package com.dtao.lms.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * ✅ Represents a single MCQ question within an Exam
 * Compatible with StudentExamService (includes internal ID for mapping)
 */
public class Question {

    @Id
    private String id = new ObjectId().toString(); // 🔹 Unique ID for internal mapping

    @Field("question_text")
    private String question;

    @Field("option_a")
    private String optionA;

    @Field("option_b")
    private String optionB;

    @Field("option_c")
    private String optionC;

    @Field("option_d")
    private String optionD;

    @Field("correct_answer")
    private String answer;

    @Field("explanation")
    private String explanation;

    // 🧱 No-Args Constructor
    public Question() {}

    // 🧱 All-Args Constructor
    public Question(String id, String question, String optionA, String optionB,
                    String optionC, String optionD, String answer, String explanation) {
        this.id = id;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.answer = answer;
        this.explanation = explanation;
    }

    // 🧱 Custom Constructor (you already had)
    public Question(String question, String optionA, String optionB,
                    String optionC, String optionD, String answer, String explanation) {
        this.id = new ObjectId().toString();
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.answer = answer;
        this.explanation = explanation;
    }

    // 🧩 Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
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
        return "Question{" +
                "id='" + id + '\'' +
                ", question='" + question + '\'' +
                ", optionA='" + optionA + '\'' +
                ", optionB='" + optionB + '\'' +
                ", optionC='" + optionC + '\'' +
                ", optionD='" + optionD + '\'' +
                ", answer='" + answer + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }

    // 🧱 Manual Builder (replacement for Lombok @Builder)
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String question;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String answer;
        private String explanation;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder optionA(String optionA) {
            this.optionA = optionA;
            return this;
        }

        public Builder optionB(String optionB) {
            this.optionB = optionB;
            return this;
        }

        public Builder optionC(String optionC) {
            this.optionC = optionC;
            return this;
        }

        public Builder optionD(String optionD) {
            this.optionD = optionD;
            return this;
        }

        public Builder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public Question build() {
            return new Question(id, question, optionA, optionB, optionC, optionD, answer, explanation);
        }
    }
}
