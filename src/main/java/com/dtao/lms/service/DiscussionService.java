package com.dtao.lms.service;

import com.dtao.lms.dto.CreateQuestionRequest;
import com.dtao.lms.dto.CreateReplyRequest;
import com.dtao.lms.model.DiscussionQuestion;
import com.dtao.lms.model.DiscussionReply;
import com.dtao.lms.repo.DiscussionQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DiscussionService {

    private static final Logger log = LoggerFactory.getLogger(DiscussionService.class);

    @Autowired
    private DiscussionQuestionRepository questionRepository;

    // 🆕 XP events (optional)
    @Autowired(required = false)
    private XpEventService xpEventService;

    // ----------------------------------------------------------
    // 🧩 CREATE QUESTION
    // ----------------------------------------------------------
    public DiscussionQuestion createQuestion(String userEmail, CreateQuestionRequest req) {
        if (userEmail == null || userEmail.isBlank() || req == null) {
            throw new IllegalArgumentException("userEmail and request are required");
        }

        DiscussionQuestion question = new DiscussionQuestion();
        question.setUserEmail(userEmail);
        question.setCourseId(req.getCourseId());
        question.setQuestionText(req.getQuestionText());

        DiscussionQuestion saved = questionRepository.save(question);

        // 🧩 Add XP event for question (best-effort)
        try {
            if (xpEventService != null) {
                xpEventService.addXpEvent(
                        userEmail,
                        "DISCUSSION",
                        5,
                        null,
                        saved.getId(),
                        null,
                        "Posted a question"
                );
            }
        } catch (Exception e) {
            log.warn("XP event for question failed for {}: {}", userEmail, e.getMessage());
        }

        return saved;
    }

    // ----------------------------------------------------------
    // 💬 ADD REPLY
    // ----------------------------------------------------------
    public DiscussionQuestion addReply(String userEmail, CreateReplyRequest req) {
        if (userEmail == null || userEmail.isBlank() || req == null || req.getQuestionId() == null) {
            throw new IllegalArgumentException("userEmail and valid request with questionId are required");
        }

        Optional<DiscussionQuestion> optional = questionRepository.findById(req.getQuestionId());
        if (optional.isEmpty()) return null;

        DiscussionQuestion question = optional.get();

        DiscussionReply reply = new DiscussionReply(userEmail, req.getReplyText());

        List<DiscussionReply> replies = question.getReplies();
        if (replies == null) {
            replies = new ArrayList<>();
        }
        replies.add(reply);
        question.setReplies(replies);

        DiscussionQuestion saved = questionRepository.save(question);

        // 🧩 Add XP event for reply (best-effort)
        try {
            if (xpEventService != null) {
                xpEventService.addXpEvent(
                        userEmail,
                        "DISCUSSION",
                        5,
                        null,
                        req.getQuestionId(),
                        null,
                        "Posted a reply"
                );
            }
        } catch (Exception e) {
            log.warn("XP event for reply failed for {} on question {}: {}", userEmail, req.getQuestionId(), e.getMessage());
        }

        return saved;
    }

    // ----------------------------------------------------------
    // 📘 GET ALL QUESTIONS FOR COURSE
    // ----------------------------------------------------------
    public List<DiscussionQuestion> getQuestionsByCourse(String courseId) {
        if (courseId == null) return List.of();
        return questionRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    // ----------------------------------------------------------
    // 🔧 Utility to prevent long text spam in XP reason
    // ----------------------------------------------------------
    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 40 ? text.substring(0, 37) + "..." : text;
    }
}
