package com.dtao.lms.service;

import com.dtao.lms.model.XpEvent;
import com.dtao.lms.repo.XpEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class XpEventService {

    private static final Logger log = LoggerFactory.getLogger(XpEventService.class);

    private final XpEventRepository xpEventRepository;

    public XpEventService(XpEventRepository xpEventRepository) {
        this.xpEventRepository = xpEventRepository;
    }

    /**
     * ✅ Add a new XP Event (prevents duplicates)
     */
    public boolean addXpEvent(String email, String type, int score,
                              String videoId, String questionId, String courseId, String message) {
        if (email == null || email.isBlank() || type == null) return false;

        try {
            // 🧠 Prevent duplicate XP for same user + type + entity (video/question/course)
            if (type.equalsIgnoreCase("VIDEO") && videoId != null) {
                if (xpEventRepository.countByEmailAndTypeAndVideoId(email, type, videoId) > 0) return false;
            }
            if (type.equalsIgnoreCase("DISCUSSION") && questionId != null) {
                if (xpEventRepository.countByEmailAndTypeAndQuestionId(email, type, questionId) > 0) return false;
            }
            if (type.equalsIgnoreCase("COURSE") && courseId != null) {
                if (xpEventRepository.countByEmailAndTypeAndCourseId(email, type, courseId) > 0) return false;
            }

            XpEvent event = new XpEvent(email, type, score, videoId, questionId, courseId, message);
            xpEventRepository.save(event);
            log.info("XP added for {} [{}] +{} points ({})", email, type, score, message);
            return true;
        } catch (Exception e) {
            log.error("Error adding XP event for {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Get all XP events for a user
     */
    public List<XpEvent> getXpHistory(String email) {
        return xpEventRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    /**
     * ✅ Compute total XP for a user
     */
    public int getTotalXp(String email) {
        return xpEventRepository.findByEmailOrderByCreatedAtDesc(email)
                .stream().mapToInt(XpEvent::getScore).sum();
    }
}
