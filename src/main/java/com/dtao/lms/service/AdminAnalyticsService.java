package com.dtao.lms.service;

import com.dtao.lms.dto.AdminProgressSummary;
import com.dtao.lms.model.Course;
import com.dtao.lms.model.User;
import com.dtao.lms.model.VideoProgress;
import com.dtao.lms.repo.CourseRepository;
import com.dtao.lms.repo.UserRepository;
import com.dtao.lms.repo.VideoProgressRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminAnalyticsService - computes aggregated metrics for the Admin Dashboard
 */
@Service
public class AdminAnalyticsService {

    private final UserRepository userRepo;
    private final VideoProgressRepository progressRepo;
    private final CourseRepository courseRepo;

    public AdminAnalyticsService(UserRepository userRepo,
                                 VideoProgressRepository progressRepo,
                                 CourseRepository courseRepo) {
        this.userRepo = userRepo;
        this.progressRepo = progressRepo;
        this.courseRepo = courseRepo;
    }

    public AdminProgressSummary buildStudentProgressAnalytics() {
        List<User> students = userRepo.findByRoleIgnoreCase("STUDENT");
        List<VideoProgress> progressList = progressRepo.findAll();
        List<Course> courses = courseRepo.findAll();

        int totalStudents = students.size();

        // Group by student email
        Map<String, List<VideoProgress>> byStudent = progressList.stream()
                .collect(Collectors.groupingBy(VideoProgress::getEmail));

        // Compute per-student completion %
        Map<String, Double> studentCompletion = new HashMap<>();
        for (Map.Entry<String, List<VideoProgress>> entry : byStudent.entrySet()) {
            List<VideoProgress> vids = entry.getValue();
            long completed = vids.stream().filter(VideoProgress::isCompleted).count();
            double percent = vids.isEmpty() ? 0.0 : (completed * 100.0 / vids.size());
            studentCompletion.put(entry.getKey(), percent);
        }

        double averageProgress = 0.0;
        if (!studentCompletion.isEmpty()) {
            averageProgress = studentCompletion.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        // Top 5 students
        List<AdminProgressSummary.TopStudent> topStudents = studentCompletion.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    User u = students.stream()
                            .filter(st -> e.getKey().equalsIgnoreCase(st.getEmail()))
                            .findFirst().orElse(null);
                    String name = (u != null) ? u.getName() : e.getKey();
                    return new AdminProgressSummary.TopStudent(name, e.getKey(), Math.round(e.getValue()));
                })
                .collect(Collectors.toList());

        // Course-level averages (rough estimate)
        Map<String, Double> courseCompletion = new HashMap<>();
        for (Course c : courses) {
            List<VideoProgress> courseVids = progressList.stream()
                    .filter(v -> v.getVideoId() != null && v.getVideoId().startsWith(c.getId()))
                    .collect(Collectors.toList());
            if (courseVids.isEmpty()) continue;
            long completed = courseVids.stream().filter(VideoProgress::isCompleted).count();
            double percent = (completed * 100.0 / courseVids.size());
            courseCompletion.put(c.getTitle(), percent);
        }

        List<AdminProgressSummary.CourseAvg> perCourse = courseCompletion.entrySet().stream()
                .map(e -> new AdminProgressSummary.CourseAvg(e.getKey(), Math.round(e.getValue())))
                .collect(Collectors.toList());

        AdminProgressSummary summary = new AdminProgressSummary();
        summary.setTotalStudents(totalStudents);
        summary.setAverageProgress(Math.round(averageProgress));
        summary.setTopStudents(topStudents);
        summary.setPerCourse(perCourse);

        return summary;
    }
}
