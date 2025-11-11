package com.dtao.lms.service;

import com.dtao.lms.dto.LeaderboardResponse;
import com.dtao.lms.model.LeaderboardAudit;
import com.dtao.lms.model.LeaderboardEntry;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.ExamResultRepository;
import com.dtao.lms.repo.LeaderboardAuditRepository;
import com.dtao.lms.repo.LikeRepository;
import com.dtao.lms.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private final ProgressService progressService;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final LeaderboardAuditRepository auditRepository;
    private final ExamResultRepository examResultRepository;

    // 🆕 XP events service (optional)
    @Autowired(required = false)
    private XpEventService xpEventService;

    // ✅ Added manual constructor (replaces Lombok @RequiredArgsConstructor)
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

    /**
     * 📚 Build leaderboard for a specific course.
     */
    public LeaderboardResponse generateLeaderboard(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("courseId required");
        }

        List<User> students = userRepository.findByRoleIgnoreCase("STUDENT");
        if (students.isEmpty()) {
            return new LeaderboardResponse(courseId, Collections.emptyList());
        }

        Map<String, Double> progressMap = progressService.getLeaderboardProgress(courseId);
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (User student : students) {
            String email = student.getEmail();
            String name = student.getName();
            double progress = progressMap.getOrDefault(email, 0.0);
            long likes = likeRepository.countByEmailAndType(email, com.dtao.lms.model.LikeType.LIKE);

            // Build entry without gamification fields
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setEmail(email);
            entry.setName(name);
            entry.setProgressPercent(progress);
            entry.setTotalLikes(likes);
            entries.add(entry);
        }

        // Sort by progress → likes → name
        entries.sort(Comparator
                .comparingDouble(LeaderboardEntry::getProgressPercent).reversed()
                .thenComparingLong(LeaderboardEntry::getTotalLikes).reversed()
                .thenComparing(LeaderboardEntry::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        int rank = 1;
        for (LeaderboardEntry e : entries) {
            e.setRank(rank++);
        }

        log.info("Generated leaderboard for courseId={} with {} entries", courseId, entries.size());
        return new LeaderboardResponse(courseId, entries);
    }

    /**
     * 🌍 Generate global leaderboard (across all courses).
     */
    public LeaderboardResponse generateGlobalLeaderboard() {
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

            long likes = likeRepository.countByEmailAndType(email, com.dtao.lms.model.LikeType.LIKE);

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
                log.warn("Failed to attach XP info for {}: {}", email, ex.getMessage());
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

        log.info("Generated global leaderboard with {} entries", entries.size());
        return new LeaderboardResponse("GLOBAL", entries);
    }

    public List<User> getAllStudents() {
        return userRepository.findByRoleIgnoreCase("STUDENT");
    }

    public int getXpForStudent(String email) {
        try {
            if (xpEventService != null) return xpEventService.getTotalXp(email);
        } catch (Exception e) {
            log.warn("Failed to fetch XP for {} : {}", email, e.getMessage());
        }
        return 0;
    }

    public Map<String, Object> resetLeaderboardData(String scope, String note) {
        try {
            log.warn("Admin triggered leaderboard reset for scope={}", scope);
            String adminEmail = "unknown";
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                    adminEmail = auth.getName();
                }
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
            log.error("Error during leaderboard reset: {}", e.getMessage());
            throw new RuntimeException("Failed to reset leaderboard data.");
        }
    }

    public List<LeaderboardEntry> getTopStudentsGlobal(int limit) {
        LeaderboardResponse global = generateGlobalLeaderboard();
        if (global == null || global.getEntries() == null) return List.of();

        return global.getEntries().stream()
                .sorted(Comparator.comparingInt(LeaderboardEntry::getRank))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    public List<LeaderboardAudit> getAllAuditLogs() {
        return auditRepository.findAllByOrderByResetAtDesc();
    }

    public LeaderboardResponse generateExamLeaderboard(String examId) {
        log.info("📘 Generating leaderboard for examId={}", examId);

        List<com.dtao.lms.model.ExamResult> results;
        try {
            results = examResultRepository.findByExamIdAndStatus(examId, "COMPLETED");
        } catch (Exception e) {
            log.error("💥 Failed to fetch exam results for examId={}: {}", examId, e.getMessage());
            return new LeaderboardResponse(examId, List.of());
        }

        if (results == null || results.isEmpty()) {
            log.warn("⚠️ No results found for examId={}", examId);
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
                .collect(Collectors.toList());

        entries.sort(Comparator
                .comparingDouble(LeaderboardEntry::getProgressPercent).reversed()
                .thenComparing(LeaderboardEntry::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        int rank = 1;
        for (LeaderboardEntry e : entries) {
            e.setRank(rank++);
        }

        log.info("✅ Generated exam leaderboard with {} entries", entries.size());
        return new LeaderboardResponse(examId, entries);
    }
}
