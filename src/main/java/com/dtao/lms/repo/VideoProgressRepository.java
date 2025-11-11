package com.dtao.lms.repo;

import com.dtao.lms.model.VideoProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoProgressRepository extends MongoRepository<VideoProgress, String> {

    // 🟢 Basic queries
    Optional<VideoProgress> findByEmailAndVideoId(String email, String videoId);

    // 🟢 Defensive duplicate handling — find all if duplicates exist
    List<VideoProgress> findAllByEmailAndVideoId(String email, String videoId);

    List<VideoProgress> findByEmailAndVideoIdIn(String email, List<String> videoIds);
    List<VideoProgress> findByEmailAndVideoIdStartsWith(String email, String prefix);
    List<VideoProgress> findByEmail(String email);

    // 🟢 Leaderboard and progress queries
    long countByEmailAndCompletedTrue(String email);

    @Query(value = "{ 'videoId': { $in: ?1 }, 'completed': true }", fields = "{ 'email' : 1 }")
    List<VideoProgress> findCompletedByVideoIds(List<String> videoIds);

    List<VideoProgress> findByVideoIdStartsWithAndCompletedTrue(String prefix);

    // ✅ Dashboard Queries

    // 🔹 Count completed videos (case-insensitive match)
    @Query(value = "{ 'email': { $regex: ?0, $options: 'i' }, 'completed': true }", count = true)
    long countCompletedVideosByEmail(String email);

    // 🔹 Get recent completed videos (limit 5)
    @Query(value = "{ 'email': { $regex: ?0, $options: 'i' }, 'completed': true }",
            sort = "{ 'updatedAt': -1 }",
            fields = "{ 'videoId': 1, 'videoTitle': 1 }")
    List<VideoProgress> findRecentCompletedVideos(String email);

    // 🔹 Get progress ratios for average progress calculation
    @Query(value = "{ 'email': { $regex: ?0, $options: 'i' }, 'duration': { $gt: 0 } }",
            fields = "{ 'lastPosition': 1, 'duration': 1, 'completed': 1 }")
    List<VideoProgress> findProgressRatioByEmail(String email);

    // ✅ Fetch recent completed videos with both videoId and title (for dashboard)
    @Query(value = "{ 'email': ?0, 'completed': true }", sort = "{ 'updatedAt': -1 }")
    List<VideoProgress> findRecentCompletedVideosWithTitle(String email);
}
