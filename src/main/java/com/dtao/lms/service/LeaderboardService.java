// LeaderboardService.java
package com.dtao.lms.service;

import com.dtao.lms.dto.LeaderboardResponse;
import com.dtao.lms.model.*;
import com.dtao.lms.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🏆 LeaderboardService
 * Optimized for async updates & caching.
 * Automatically triggered when students complete exams.
 */
@Service
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private final ProgressService progressService;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final LeaderboardAuditRepository auditRepository;
    private final ExamResultRepository examResultRepository;

    @Autowired(required = false)
    private XpEventService xpEventService;

    @Autowired
    public LeaderboardService(
            ProgressService progressService,
            LikeRepository likeRepository,
            UserRepository userRepository,
            LeaderboardAuditRepository auditRepository,
            ExamResultRepository examResultRepository
    ) {
        this.progressService = progressService;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.examResultRepository = examResultRepository;
    }

    /* ============================================================
     * 1️⃣ Async leaderboard updater (called after exam submit)
     * ============================================================ */
    @Async("taskExecutor")
    @CacheEvict(value = {"globalLeaderboard", "examLeaderboards"}, allEntries = true)
    public void updateLeaderboardAsync(ExamResult result) {
        try {
            if (result == null) return;

            String email = result.getStudentEmail();
            double newScore = result.getPercentage();

            // 🧮 Update XP if service available
            if (xpEventService != null) {
                try {
                    int xpEarned = (int) Math.round(newScore); // 1 XP per percentage point
                    xpEventService.addXp(email, xpEarned, "Exam Completed", "Exam: " + result.getExamName());
                    log.info("🎖 XP +{} added for {}", xpEarned, email);
                } catch (Exception e) {
                    log.warn("⚠️ Failed XP update for {}: {}", email, e.getMessage());
                }
            }

            // 🏆 Refresh the student’s leaderboard entry
            LeaderboardResponse updated = generateGlobalLeaderboard();
            log.info("✅ [Async] Leaderboard refreshed after exam result for {}", email);

        } catch (Exception e) {
            log.error("💥 Async leaderboard update failed: {}", e.getMessage());
        }
    }

    // ===========================
    // ✅ Backward compatibility
    // ===========================
    // Older controllers may call generateLeaderboard(courseId).
    // Keep that signature and delegate to the new internal method.
    public LeaderboardResponse generateLeaderboard(String courseId) {
        return generateCourseLeaderboard(courseId);
    }

    // New internal name — preserves previous behavior by delegating
    // to generateExamLeaderboard (examId-based) to avoid breaking logic.
    public LeaderboardResponse generateCourseLeaderboard(String courseId) {
        // If courseId maps to an examId in your system, this will work.
        // Otherwise, you can replace this delegation with course-specific logic.
        return generateExamLeaderboard(courseId);
    }

    /* ============================================================
     * 2️⃣ Global leaderboard (cached)
     * ============================================================ */
    @Cacheable(value = "globalLeaderboard")
    public LeaderboardResponse generateGlobalLeaderboard() {
        log.info("⚙️ Building global leaderboard (cache miss, computing fresh...)");

        List<User> students = userRepository.findByRoleIgnoreCase("STUDENT");
        if (students.isEmpty()) {
            return new LeaderboardResponse("GLOBAL", Collections.emptyList());
        }

        List<LeaderboardEntry> entries = students.stream().map(student -> {
            String email = student.getEmail();
            String name = student.getName();

            double avgPercent = 0.0;
            try {
                avgPercent = progressService.getAverageProgressForUser(email);
            } catch (Exception ex) {
                log.warn("Failed to get average progress for {}: {}", email, ex.getMessage());
            }

            long likes = likeRepository.countByEmailAndType(email, LikeType.LIKE);

            LeaderboardEntry e = new LeaderboardEntry();
            e.setEmail(email);
            e.setName(name);
            e.setProgressPercent(avgPercent);
            e.setTotalLikes(likes);

            try {
                if (xpEventService != null) {
                    int xp = xpEventService.getTotalXp(email);
                    e.setXp(xp);
                    e.setLevel((xp / 100) + 1);
                    String badge = xp >= 500 ? "Pro Learner 🥇" :
                            xp >= 200 ? "Active Learner 🥈" :
                                    "New Learner 🐣";
                    e.setBadge(badge);
                }
            } catch (Exception ex) {
                log.warn("XP fetch failed for {}: {}", email, ex.getMessage());
            }

            return e;
        }).collect(Collectors.toList());

        entries.sort(Comparator
                .comparingInt(LeaderboardEntry::getXp).reversed()
                .thenComparingDouble(LeaderboardEntry::getProgressPercent).reversed()
                .thenComparingLong(LeaderboardEntry::getTotalLikes).reversed()
                .thenComparing(LeaderboardEntry::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        int rank = 1;
        for (LeaderboardEntry e : entries) {
            e.setRank(rank++);
        }

        log.info("✅ Global leaderboard built with {} entries", entries.size());
        return new LeaderboardResponse("GLOBAL", entries);
    }

    /* ============================================================
     * 3️⃣ Exam leaderboard (cached)
     * ============================================================ */
    @Cacheable(value = "examLeaderboards", key = "#examId")
    public LeaderboardResponse generateExamLeaderboard(String examId) {
        log.info("📘 Building leaderboard for examId={} (cache miss)", examId);

        List<ExamResult> results;
        try {
            results = examResultRepository.findByExamIdAndStatus(examId, "COMPLETED");
        } catch (Exception e) {
            log.error("💥 Failed to fetch exam results: {}", e.getMessage());
            return new LeaderboardResponse(examId, List.of());
        }

        if (results == null || results.isEmpty()) {
            log.warn("⚠️ No results for examId={}", examId);
            return new LeaderboardResponse(examId, List.of());
        }

        List<LeaderboardEntry> entries = results.stream()
                .map(r -> {
                    LeaderboardEntry entry = new LeaderboardEntry();
                    entry.setEmail(r.getStudentEmail());
                    entry.setName(r.getStudentName());
                    entry.setProgressPercent(r.getPercentage());
                    entry.setTotalLikes(0);
                    return entry;
                })
                .sorted(Comparator
                        .comparingDouble(LeaderboardEntry::getProgressPercent).reversed()
                        .thenComparing(LeaderboardEntry::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        int rank = 1;
        for (LeaderboardEntry e : entries) e.setRank(rank++);

        log.info("✅ Exam leaderboard generated with {} entries", entries.size());
        return new LeaderboardResponse(examId, entries);
    }

    /* ============================================================
     * 4️⃣ Admin utilities and misc
     * ============================================================ */
    public List<User> getAllStudents() {
        return userRepository.findByRoleIgnoreCase("STUDENT");
    }

    public int getXpForStudent(String email) {
        try {
            if (xpEventService != null) return xpEventService.getTotalXp(email);
        } catch (Exception e) {
            log.warn("Failed XP fetch for {} : {}", email, e.getMessage());
        }
        return 0;
    }

    public Map<String, Object> resetLeaderboardData(String scope, String note) {
        try {
            log.warn("Admin triggered leaderboard reset for scope={}", scope);
            String adminEmail = "unknown";
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) adminEmail = auth.getName();
            } catch (Exception ignored) {}

            LeaderboardAudit audit = new LeaderboardAudit(adminEmail, scope, note);
            auditRepository.save(audit);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Leaderboard data reset successfully.");
            result.put("scope", scope);
            result.put("note", note);
            result.put("auditId", audit.getId());
            result.put("timestamp", audit.getResetAt());
            return result;

        } catch (Exception e) {
            log.error("Error resetting leaderboard: {}", e.getMessage());
            throw new RuntimeException("Leaderboard reset failed");
        }
    }

    public List<LeaderboardAudit> getAllAuditLogs() {
        return auditRepository.findAllByOrderByResetAtDesc();
    }

    public List<LeaderboardEntry> getTopStudentsGlobal(int limit) {
        LeaderboardResponse global = generateGlobalLeaderboard();
        if (global == null || global.getEntries() == null) return List.of();
        return global.getEntries().stream()
                .sorted(Comparator.comparingInt(LeaderboardEntry::getRank))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }
}
