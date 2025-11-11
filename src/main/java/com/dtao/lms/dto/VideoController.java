package com.dtao.lms.dto;//package com.dtao.lms.dto;
//
//import com.dtao.lms.dto.VideoDTO;
//import com.dtao.lms.model.Video;
//import com.dtao.lms.repo.VideoRepository;
//import org.springframework.http.ResponseEntity;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.*;
//
//import java.net.URL;
//import java.util.Optional;
//
///**
// * VideoController: provides sanitized metadata for frontend
// * - GET /api/videos/{id} returns VideoDTO
// *   - If source is YouTube -> sets youtubeId and removes direct videoUrl/source from response.
// *   - Otherwise returns a safe 'source' that the frontend can use (e.g. will call /api/videos/{id}/stream).
// */
//@RestController
//@RequestMapping("/api/videos")
//@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
//public class VideoController {
//
//    private final VideoRepository videoRepo;
//
//    public VideoController(VideoRepository videoRepo) {
//        this.videoRepo = videoRepo;
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<?> getVideoSanitized(@PathVariable("id") String id) {
//        Optional<Video> maybe = videoRepo.findById(id);
//        if (maybe.isEmpty()) return ResponseEntity.notFound().build();
//        Video v = maybe.get();
//
//        VideoDTO dto = new VideoDTO();
//        dto.setId(v.getId());
//        dto.setTitle(v.getTitle());
//        dto.setDescription(v.getDescription());
//        dto.setContentType(v.getContentType());
//        dto.setCreatedAt(v.getCreatedAt());
//        dto.setUpdatedAt(v.getUpdatedAt());
//
//        String path = v.getVideoUrl();
//        if (!StringUtils.hasText(path)) path = v.getSourceUrl();
//
//        if (path != null) {
//            try {
//                URL u = new URL(path);
//                String host = u.getHost().toLowerCase();
//                if (host.contains("youtube.com") || host.contains("youtu.be")) {
//                    // extract video id
//                    String youtubeId = null;
//                    if (host.contains("youtube.com")) {
//                        youtubeId = u.getQuery() == null ? null : getQueryParam(u.getQuery(), "v");
//                    } else if (host.contains("youtu.be")) {
//                        String p = u.getPath();
//                        if (p != null && p.length() > 1) youtubeId = p.substring(1);
//                    }
//                    dto.setYoutubeId(youtubeId);
//                    dto.setSource(null); // hide raw url
//                    return ResponseEntity.ok(dto);
//                }
//            } catch (Exception ignore) {}
//        }
//
//        // non-youtube: tell frontend to use proxied stream endpoint
//        dto.setSource("/api/videos/" + v.getId() + "/stream");
//        return ResponseEntity.ok(dto);
//    }
//
//    private String getQueryParam(String query, String key) {
//        if (query == null || key == null) return null;
//        String[] parts = query.split("&");
//        for (String p : parts) {
//            int idx = p.indexOf('=');
//            if (idx > 0) {
//                String k = p.substring(0, idx);
//                String val = p.substring(idx + 1);
//                if (k.equals(key)) return val;
//            }
//        }
//        return null;
//    }
//}
