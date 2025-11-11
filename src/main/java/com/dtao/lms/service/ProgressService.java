package com.dtao.lms.service;

import com.dtao.lms.dto.CourseProgressResponse;
import com.dtao.lms.model.User;
import com.dtao.lms.model.Video;
import com.dtao.lms.model.VideoProgress;
import com.dtao.lms.repo.UserRepository;
import com.dtao.lms.repo.VideoProgressRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressService.class);

    private final VideoProgressRepository progressRepo;
    private final CourseService courseService;

    @Autowired
    private UserRepository userRepository;

    // 🆕 XP events (decoupled from old GamificationService)
    @Autowired(required = false)
    private XpEventService xpEventService;

    private final Counter createCounter;
    private final Counter updateCounter;
    private final Counter completeCounter;

    public ProgressService(VideoProgressRepository progressRepo,
                           CourseService courseService,
                           ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.progressRepo = progressRepo;
        this.courseService = courseService;

        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry != null) {
            this.createCounter = Counter.builder("lms.video_progress.writes")
                    .description("Video progress write operations (create)")
                    .tag("type", "create")
                    .register(registry);

            this.updateCounter = Counter.builder("lms.video_progress.writes")
                    .description("Video progress write operations (update)")
                    .tag("type", "update")
                    .register(registry);

            this.completeCounter = Counter.builder("lms.video_progress.writes")
                    .description("Video progress write operations (complete)")
                    .tag("type", "complete")
                    .register(registry);
            log.info("Micrometer MeterRegistry found: progress counters enabled");
        } else {
            this.createCounter = null;
            this.updateCounter = null;
            this.completeCounter = null;
            log.info("No MeterRegistry available: progress counters disabled");
        }
    }

    private void incCreate() { if (createCounter != null) createCounter.increment(); }
    private void incUpdate() { if (updateCounter != null) updateCounter.increment(); }
    private void incComplete() { if (completeCounter != null) completeCounter.increment(); }

    // ----------------------------------------------------------
    // upsertProgress (safe version that cleans duplicates and avoids races)
    // ----------------------------------------------------------
    @Transactional
    public VideoProgress upsertProgress(String email, String videoId, Double lastPosition, Double duration, Boolean completed) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email required");
        if (videoId == null || videoId.isBlank()) throw new IllegalArgumentException("videoId required");

        try {
            List<VideoProgress> list = progressRepo.findAllByEmailAndVideoId(email, videoId);
            VideoProgress progress;
            boolean changed = false;

            if (list == null || list.isEmpty()) {
                progress = new VideoProgress(
                        email,
                        videoId,
                        sanitizeTime(lastPosition, 0.0),
                        sanitizeTime(duration, 0.0),
                        completed != null && completed
                );

                try {
                    Video v = courseService.getVideoById(videoId);
                    if (v != null && v.getTitle() != null) progress.setVideoTitle(v.getTitle());
                } catch (Exception ignored) {}

                progress.setUpdatedAt(Instant.now());
                VideoProgress saved = progressRepo.save(progress);
                incCreate();
                try { debugProgressEvent("upsertProgress", email, videoId, saved.isCompleted(), false); } catch (Exception ignored) {}
                return saved;
            }

            progress = list.get(0);
            if (list.size() > 1) {
                log.warn("Duplicate progress entries for {}/{} → cleaning up {}", email, videoId, list.size() - 1);
                for (int i = 1; i < list.size(); i++) {
                    try {
                        progressRepo.delete(list.get(i));
                    } catch (Exception e) {
                        log.warn("Failed to delete duplicate progress id {}: {}", list.get(i).getId(), e.getMessage());
                    }
                }
            }

            if (isFinite(lastPosition)) {
                double existingPos = progress.getLastPosition() == null ? 0.0 : progress.getLastPosition();
                if (lastPosition > existingPos) {
                    progress.setLastPosition(lastPosition);
                    changed = true;
                }
            }

            if (isFinite(duration)) {
                double newDur = Math.max(0.0, duration);
                if (progress.getDuration() == null || Double.compare(progress.getDuration(), newDur) != 0) {
                    progress.setDuration(newDur);
                    changed = true;
                }
            }

            if (completed != null && completed && !progress.isCompleted()) {
                progress.setCompleted(true);
                changed = true;
            }

            if ((progress.getVideoTitle() == null || progress.getVideoTitle().isBlank())) {
                try {
                    Video v = courseService.getVideoById(videoId);
                    if (v != null && v.getTitle() != null) {
                        progress.setVideoTitle(v.getTitle());
                        changed = true;
                    }
                } catch (Exception ignored) {}
            }

            if (changed) {
                progress.setUpdatedAt(Instant.now());
                VideoProgress saved = progressRepo.save(progress);
                incUpdate();

                // ✅ Award XP when video first marked completed
                try {
                    if (xpEventService != null && saved.isCompleted()) {
                        xpEventService.addXpEvent(
                                email,
                                "VIDEO",
                                10,
                                videoId,
                                null,
                                null,
                                "Completed video: " + (saved.getVideoTitle() != null ? saved.getVideoTitle() : videoId)
                        );
                    }
                } catch (Exception e) {
                    log.warn("⚠️ XP event failed in upsertProgress for {} video {}: {}", email, videoId, e.getMessage());
                }

                try { debugProgressEvent("upsertProgress", email, videoId, saved.isCompleted(), xpEventService != null && saved.isCompleted()); } catch (Exception ignored) {}
                return saved;
            }

            try { debugProgressEvent("upsertProgress", email, videoId, progress.isCompleted(), false); } catch (Exception ignored) {}
            return progress;

        } catch (Exception ex) {
            log.error("upsertProgress failed for {}/{} : {}", email, videoId, ex.getMessage());
            throw ex;
        }
    }

    // ----------------------------------------------------------
    // markCompleted — now emits XP event via XpEventService
    // ----------------------------------------------------------
    @Transactional
    public VideoProgress markCompleted(String email, String videoId) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email required");
        if (videoId == null || videoId.isBlank()) throw new IllegalArgumentException("videoId required");

        Optional<VideoProgress> maybe = progressRepo.findByEmailAndVideoId(email, videoId);

        if (maybe.isEmpty()) {
            VideoProgress p = new VideoProgress(email, videoId, 0.0, 0.0, true);

            try {
                Video video = courseService.getVideoById(videoId);
                if (video != null && video.getTitle() != null) {
                    p.setVideoTitle(video.getTitle());
                }
            } catch (Exception e) {
                log.warn("Could not set video title for {}: {}", videoId, e.getMessage());
            }

            p.setUpdatedAt(Instant.now());
            VideoProgress saved = progressRepo.save(p);
            incComplete();

            // 🧩 Add XP event (first-time completion)
            try {
                if (xpEventService != null) {
                    xpEventService.addXpEvent(
                            email,
                            "VIDEO",
                            10,
                            videoId,
                            null,
                            null,
                            "Completed video: " + (p.getVideoTitle() != null ? p.getVideoTitle() : videoId)
                    );
                }
            } catch (Exception e) {
                log.warn("XP event failed for {} video {}: {}", email, videoId, e.getMessage());
            }

            try { debugProgressEvent("markCompleted", email, videoId, true, xpEventService != null); } catch (Exception ignored) {}
            return saved;

        } else {
            VideoProgress p = maybe.get();

            if (p.isCompleted()) {
                log.info("User {} already completed video {} — skipping duplicate XP event", email, videoId);
                try { debugProgressEvent("markCompleted", email, videoId, true, false); } catch (Exception ignored) {}
                return p;
            }

            p.setCompleted(true);
            if (p.getVideoTitle() == null || p.getVideoTitle().isBlank()) {
                try {
                    Video video = courseService.getVideoById(videoId);
                    if (video != null && video.getTitle() != null) {
                        p.setVideoTitle(video.getTitle());
                    }
                } catch (Exception e) {
                    log.warn("Could not set video title for {}: {}", videoId, e.getMessage());
                }
            }
            p.setUpdatedAt(Instant.now());
            VideoProgress saved = progressRepo.save(p);
            incComplete();

            // 🧩 Add XP event (only once)
            try {
                if (xpEventService != null) {
                    xpEventService.addXpEvent(
                            email,
                            "VIDEO",
                            10,
                            videoId,
                            null,
                            null,
                            "Completed video: " + (p.getVideoTitle() != null ? p.getVideoTitle() : videoId)
                    );
                }
            } catch (Exception e) {
                log.warn("XP event failed for {} video {}: {}", email, videoId, e.getMessage());
            }

            try { debugProgressEvent("markCompleted", email, videoId, true, xpEventService != null); } catch (Exception ignored) {}
            return saved;
        }
    }

    // ----------------------------------------------------------
    // computeCourseProgressForUser
    // ----------------------------------------------------------
    public CourseProgressResponse computeCourseProgressForUser(String email, String courseId) {
        if (email == null || email.isBlank() || courseId == null || courseId.isBlank()) {
            return new CourseProgressResponse(courseId, 0, 0, Collections.emptyList());
        }

        List<Video> all = courseService.getAllVideosForCourse(courseId);
        int total = all == null ? 0 : all.size();
        if (total == 0) return new CourseProgressResponse(courseId, 0, 0, Collections.emptyList());

        List<String> videoIds = all.stream().map(Video::getId).filter(Objects::nonNull).toList();
        List<VideoProgress> progressList = progressRepo.findByEmailAndVideoIdIn(email, videoIds);

        Set<String> completedSet = progressList.stream()
                .filter(VideoProgress::isCompleted)
                .map(VideoProgress::getVideoId)
                .collect(Collectors.toSet());

        int completed = completedSet.size();

        return new CourseProgressResponse(courseId, total, completed, new ArrayList<>(completedSet));
    }

    // --- helpers ---
    private static boolean isFinite(Double v) { return v != null && !v.isNaN() && !v.isInfinite(); }
    private static double sanitizeTime(Double v, double fallback) { return (!isFinite(v)) ? fallback : Math.max(0.0, v); }

    /**
     * 🔍 DEBUG helper - logs video progress updates and XP triggers in detail.
     */
    private void debugProgressEvent(String action, String email, String videoId, boolean completed, boolean xpTriggered) {
        log.info(
                "\n=============================\n" +
                        "🎬 Progress Event Triggered\n" +
                        "Action: {}\n" +
                        "User: {}\n" +
                        "Video ID: {}\n" +
                        "Completed: {}\n" +
                        "XP Triggered: {}\n" +
                        "Timestamp: {}\n" +
                        "=============================",
                action, email, videoId, completed, xpTriggered, Instant.now()
        );
    }

    // ----------------------------------------------------------
    // 🏆 Analytics / Dashboard Methods
    // ----------------------------------------------------------
    public long getTotalCompletedVideos(String email) {
        if (email == null || email.isBlank()) return 0;
        return progressRepo.countByEmailAndCompletedTrue(email);
    }

    public double getCourseCompletionPercent(String email, String courseId) {
        if (email == null || courseId == null) return 0.0;
        List<Video> courseVideos = courseService.getAllVideosForCourse(courseId);
        if (courseVideos == null || courseVideos.isEmpty()) return 0.0;

        List<String> videoIds = courseVideos.stream().map(Video::getId).toList();
        long completedCount = progressRepo.findByEmailAndVideoIdIn(email, videoIds)
                .stream().filter(VideoProgress::isCompleted).count();

        return (completedCount == videoIds.size()) ? 100.0
                : (videoIds.isEmpty() ? 0.0 : (completedCount * 100.0 / videoIds.size()));
    }

    public Map<String, Double> getLeaderboardProgress(String courseId) {
        List<User> students = userRepository.findByRoleIgnoreCase("STUDENT");
        Map<String, Double> leaderboard = new HashMap<>();
        for (User s : students) {
            double percent = getCourseCompletionPercent(s.getEmail(), courseId);
            leaderboard.put(s.getEmail(), percent);
        }
        return leaderboard;
    }

    public double getAverageProgressForUser(String email) {
        if (email == null || email.isBlank()) return 0.0;
        try {
            var progressList = progressRepo.findProgressRatioByEmail(email);
            if (progressList == null || progressList.isEmpty()) return 0.0;

            double totalPercent = 0.0;
            int count = 0;
            for (var vp : progressList) {
                if (vp.getDuration() != null && vp.getDuration() > 0) {
                    double percent = (vp.getLastPosition() / vp.getDuration()) * 100.0;
                    totalPercent += Math.min(percent, 100.0);
                    count++;
                }
            }
            return count > 0 ? Math.round((totalPercent / count) * 100.0) / 100.0 : 0.0;
        } catch (Exception e) {
            log.error("Error calculating average progress for {}: {}", email, e.getMessage());
            return 0.0;
        }
    }

    public long getTotalVideosCompleted(String email) {
        if (email == null || email.isBlank()) return 0;
        try {
            return progressRepo.countCompletedVideosByEmail(email);
        } catch (Exception e) {
            log.error("Error counting completed videos for {}: {}", email, e.getMessage());
            return 0;
        }
    }

    public List<String> getRecentCompletedVideos(String email) {
        if (email == null || email.isBlank()) return List.of();
        try {
            return progressRepo.findRecentCompletedVideos(email)
                    .stream()
                    .map(vp -> {
                        String title = vp.getVideoTitle();
                        return (title != null && !title.isBlank()) ? title : vp.getVideoId();
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Error getting recent videos for {}: {}", email, e.getMessage());
            return List.of();
        }
    }

    /**
     * ✅ Simple helper to get progress for a given user + video
     */
    public Optional<VideoProgress> getProgress(String email, String videoId) {
        if (email == null || email.isBlank()) return Optional.empty();
        if (videoId == null || videoId.isBlank()) return Optional.empty();
        try {
            return progressRepo.findByEmailAndVideoId(email, videoId);
        } catch (Exception e) {
            log.error("Error fetching progress for {}/{}: {}", email, videoId, e.getMessage());
            return Optional.empty();
        }
    }


    /**
     * ✅ Fetch all video progress records for a given user (by email)
     */
    public List<VideoProgress> getProgressForUser(String email) {
        if (email == null || email.isBlank()) return Collections.emptyList();
        try {
            return progressRepo.findByEmail(email);
        } catch (Exception e) {
            log.error("Error fetching progress for user {}: {}", email, e.getMessage());
            return Collections.emptyList();
        }
    }
}
