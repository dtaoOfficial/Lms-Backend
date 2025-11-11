package com.dtao.lms.service;

import com.dtao.lms.model.Chapter;
import com.dtao.lms.model.Course;
import com.dtao.lms.model.Video;
import com.dtao.lms.repo.ChapterRepository;
import com.dtao.lms.repo.CourseRepository;
import com.dtao.lms.repo.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CourseService
 *
 * Provides CRUD and helper methods for Courses, Chapters and Videos.
 * Deletion methods are transactional to avoid leaving orphaned documents.
 */
@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final VideoRepository videoRepo;

    // 🆕 XP events (optional)
    @Autowired(required = false)
    private XpEventService xpEventService;

    public CourseService(CourseRepository courseRepo, ChapterRepository chapterRepo, VideoRepository videoRepo) {
        this.courseRepo = courseRepo;
        this.chapterRepo = chapterRepo;
        this.videoRepo = videoRepo;
    }

    // === COURSES ===

    public List<Course> getAllCourses() {
        return courseRepo.findAll();
    }

    public Optional<Course> getCourseById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return courseRepo.findById(id);
    }

    public Course createCourse(Course c) {
        if (c == null) throw new IllegalArgumentException("course required");
        Instant now = Instant.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        Course saved = courseRepo.save(c);
        log.debug("Created course {}", saved.getId());
        return saved;
    }

    public Optional<Course> updateCourse(String id, Course newData) {
        if (id == null || id.isBlank() || newData == null) return Optional.empty();
        return courseRepo.findById(id).map(existing -> {
            if (newData.getTitle() != null) existing.setTitle(newData.getTitle());
            if (newData.getDescription() != null) existing.setDescription(newData.getDescription());
            if (newData.getThumbnailUrl() != null) existing.setThumbnailUrl(newData.getThumbnailUrl());
            if (newData.getTags() != null) existing.setTags(newData.getTags());
            if (newData.getInstructor() != null) existing.setInstructor(newData.getInstructor());
            existing.setUpdatedAt(Instant.now());
            Course saved = courseRepo.save(existing);
            log.debug("Updated course {}", saved.getId());
            return saved;
        });
    }

    /**
     * Delete a course and all its chapters/videos (transactional).
     */
    @Transactional
    public void deleteCourse(String id) {
        if (id == null || id.isBlank()) return;
        try {
            List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderAsc(id);
            if (chapters != null && !chapters.isEmpty()) {
                for (Chapter ch : chapters) {
                    List<Video> vids = videoRepo.findByChapterIdOrderByOrderAsc(ch.getId());
                    if (vids != null && !vids.isEmpty()) {
                        videoRepo.deleteAll(vids);
                    }
                }
                chapterRepo.deleteAll(chapters);
            }

            // delete any videos not attached to a chapter but under course
            List<Video> orphans = videoRepo.findByCourseIdAndChapterIdIsNullOrderByOrderAsc(id);
            if (orphans != null && !orphans.isEmpty()) {
                videoRepo.deleteAll(orphans);
            }

            courseRepo.deleteById(id);
            log.debug("Deleted course {} and its chapters/videos", id);
        } catch (Exception ex) {
            log.error("Failed to delete course {} : {}", id, ex.getMessage(), ex);
            throw ex;
        }
    }

    // === CHAPTERS ===

    public List<Chapter> getChaptersByCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) return List.of();
        List<Chapter> list = chapterRepo.findByCourseIdOrderByOrderAsc(courseId);
        return list == null ? List.of() : list;
    }

    public Chapter createChapter(Chapter ch) {
        if (ch == null) throw new IllegalArgumentException("chapter required");
        Instant now = Instant.now();
        ch.setCreatedAt(now);
        ch.setUpdatedAt(now);
        Chapter saved = chapterRepo.save(ch);
        log.debug("Created chapter {}", saved.getId());
        return saved;
    }

    public Optional<Chapter> updateChapter(String id, Chapter newData) {
        if (id == null || id.isBlank() || newData == null) return Optional.empty();
        return chapterRepo.findById(id).map(existing -> {
            if (newData.getTitle() != null) existing.setTitle(newData.getTitle());
            if (newData.getDescription() != null) existing.setDescription(newData.getDescription());
            existing.setUpdatedAt(Instant.now());
            Chapter saved = chapterRepo.save(existing);
            log.debug("Updated chapter {}", saved.getId());
            return saved;
        });
    }

    /**
     * Delete a chapter and all its videos (transactional).
     */
    @Transactional
    public void deleteChapter(String id) {
        if (id == null || id.isBlank()) return;
        try {
            List<Video> vids = videoRepo.findByChapterIdOrderByOrderAsc(id);
            if (vids != null && !vids.isEmpty()) {
                videoRepo.deleteAll(vids);
            }
            chapterRepo.deleteById(id);
            log.debug("Deleted chapter {} and its videos", id);
        } catch (Exception ex) {
            log.error("Failed to delete chapter {} : {}", id, ex.getMessage(), ex);
            throw ex;
        }
    }

    // === VIDEOS ===

    public List<Video> getVideosByChapter(String chapterId) {
        if (chapterId == null || chapterId.isBlank()) return List.of();
        List<Video> list = videoRepo.findByChapterIdOrderByOrderAsc(chapterId);
        return list == null ? List.of() : list;
    }

    public List<Video> getVideosWithoutChapterForCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) return List.of();
        List<Video> list = videoRepo.findByCourseIdAndChapterIdIsNullOrderByOrderAsc(courseId);
        return list == null ? List.of() : list;
    }

    public Video createVideo(Video v) {
        if (v == null) throw new IllegalArgumentException("video required");
        Instant now = Instant.now();
        v.setCreatedAt(now);
        v.setUpdatedAt(now);
        Video saved = videoRepo.save(v);
        log.debug("Created video {}", saved.getId());
        return saved;
    }

    public Optional<Video> updateVideo(String id, Video newData) {
        if (id == null || id.isBlank() || newData == null) return Optional.empty();
        return videoRepo.findById(id).map(existing -> {
            if (newData.getTitle() != null) existing.setTitle(newData.getTitle());
            if (newData.getDescription() != null) existing.setDescription(newData.getDescription());
            if (newData.getVideoUrl() != null) existing.setVideoUrl(newData.getVideoUrl());
            if (newData.getContentType() != null) existing.setContentType(newData.getContentType());
            existing.setUpdatedAt(Instant.now());
            Video saved = videoRepo.save(existing);
            log.debug("Updated video {}", saved.getId());
            return saved;
        });
    }

    public void deleteVideo(String id) {
        if (id == null || id.isBlank()) return;
        try {
            videoRepo.deleteById(id);
            log.debug("Deleted video {}", id);
        } catch (Exception ex) {
            log.error("Failed to delete video {} : {}", id, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Helper to get all videos belonging to a course (videos inside chapters + videos without chapter)
     */
    public List<Video> getAllVideosForCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) return List.of();

        List<Chapter> chapters = chapterRepo.findByCourseIdOrderByOrderAsc(courseId);
        List<Video> all = new ArrayList<>();

        if (chapters != null && !chapters.isEmpty()) {
            for (Chapter ch : chapters) {
                List<Video> vids = videoRepo.findByChapterIdOrderByOrderAsc(ch.getId());
                if (vids != null && !vids.isEmpty()) all.addAll(vids);
            }
        }

        List<Video> noChapter = videoRepo.findByCourseIdAndChapterIdIsNullOrderByOrderAsc(courseId);
        if (noChapter != null && !noChapter.isEmpty()) all.addAll(noChapter);

        return all;
    }

    // ✅ NEW METHOD for video lookup by ID (used in ProgressService)
    public Video getVideoById(String videoId) {
        if (videoId == null || videoId.isBlank()) return null;
        try {
            return videoRepo.findById(videoId).orElse(null);
        } catch (Exception e) {
            log.error("Error fetching video by ID {}: {}", videoId, e.getMessage());
            return null;
        }
    }

    /**
     * 🧩 Emit course-completion XP event (100 XP) — best-effort helper.
     * Call this after you detect a user completed all videos in a course.
     */
    public void emitCourseCompletionXp(String email, String courseId) {
        if (email == null || email.isBlank() || courseId == null || courseId.isBlank()) return;
        try {
            if (xpEventService != null) {
                xpEventService.addXpEvent(
                        email,
                        "COURSE",
                        100,
                        null,
                        null,
                        courseId,
                        "Completed full course"
                );
                log.info("Emitted COURSE XP event for {} course {}", email, courseId);
            }
        } catch (Exception e) {
            log.warn("Failed to emit course completion XP for {} course {}: {}", email, courseId, e.getMessage());
        }
    }
}
