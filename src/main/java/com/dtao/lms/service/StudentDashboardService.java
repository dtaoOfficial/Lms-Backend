package com.dtao.lms.service;

import com.dtao.lms.dto.StudentDashboardStatsResponse;
import com.dtao.lms.model.User;
import com.dtao.lms.model.VideoProgress;
import com.dtao.lms.repo.EnrollmentRepository;
import com.dtao.lms.repo.UserRepository;
import com.dtao.lms.repo.VideoProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎓 StudentDashboardService
 * Collects all student statistics for dashboard:
 * - Enrollments
 * - Progress
 * - Certificates
 * - Likes
 */
@Service
public class StudentDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private VideoProgressRepository videoProgressRepository;

    @Autowired
    private CertificateService certificateService;

    /**
     * 📊 Build all dashboard stats for the given student.
     */
    public StudentDashboardStatsResponse getStudentDashboardStats(String email) {
        if (email == null || email.isBlank()) {
            return emptyStats();
        }

        email = email.trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return emptyStats();
        }

        long totalCourses = 0;
        long completedVideos = 0;
        double avgProgress = 0.0;
        long totalCertificates = 0;
        List<String> recentVideos = Collections.emptyList();
        long totalLikes = 0;

        try {
            totalCourses = enrollmentRepository.countByEmailRegex(email);
        } catch (Exception ignored) {}

        try {
            completedVideos = videoProgressRepository.countCompletedVideosByEmail(email);
        } catch (Exception ignored) {}

        try {
            List<VideoProgress> progressList = videoProgressRepository.findProgressRatioByEmail(email);
            if (progressList != null && !progressList.isEmpty()) {
                double totalPercent = 0.0;
                int count = 0;
                for (VideoProgress vp : progressList) {
                    if (vp.getDuration() != null && vp.getDuration() > 0) {
                        double percent = (vp.getLastPosition() / vp.getDuration()) * 100.0;
                        totalPercent += Math.min(percent, 100.0);
                        count++;
                    }
                }
                avgProgress = count > 0 ? (totalPercent / count) : 0.0;
            }
        } catch (Exception ignored) {}

        try {
            totalCertificates = certificateService.listCertificatesForUser(email).size();
        } catch (Exception ignored) {}

        try {
            recentVideos = videoProgressRepository.findRecentCompletedVideosWithTitle(email)
                    .stream()
                    .map(vp -> {
                        String title = vp.getVideoTitle();
                        if (title == null || title.isBlank()) {
                            return "Video ID: " + vp.getVideoId();
                        }
                        return title;
                    })
                    .limit(5)
                    .collect(Collectors.toList());

        } catch (Exception ignored) {}

        try {
            totalLikes = user.getTotalLikesGiven();
        } catch (Exception ignored) {}

        // gamification removed — return core dashboard stats only
        return new StudentDashboardStatsResponse(
                totalCourses,
                completedVideos,
                Math.round(avgProgress * 100.0) / 100.0,
                totalCertificates,
                totalLikes,
                recentVideos
        );
    }

    private StudentDashboardStatsResponse emptyStats() {
        return new StudentDashboardStatsResponse(0, 0, 0.0, 0, 0, List.of());
    }
}
