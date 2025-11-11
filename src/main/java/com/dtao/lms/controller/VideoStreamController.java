package com.dtao.lms.controller;

import com.dtao.lms.model.Video;
import com.dtao.lms.repo.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * VideoStreamController: streams local files and proxies remote video URLs (with Range support).
 * - Does NOT proxy YouTube (frontend must embed YouTube).
 * - Uses final locals for lambda capture safety.
 */
@RestController
@RequestMapping("/api/videos")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)

public class VideoStreamController {

    private final VideoRepository videoRepo;
    private static final long CHUNK_SIZE = 1024L * 1024L * 2L; // 2MB

    @Autowired
    public VideoStreamController(VideoRepository videoRepo) {
        this.videoRepo = videoRepo;
    }

    private boolean isYouTubeHost(String url) {
        if (!StringUtils.hasText(url)) return false;
        try {
            URL u = new URL(url);
            String host = u.getHost().toLowerCase();
            return host.contains("youtube.com") || host.contains("youtu.be");
        } catch (Exception ex) {
            return false;
        }
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<StreamingResponseBody> streamVideo(@PathVariable("id") String id,
                                                             @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            Optional<Video> maybe = videoRepo.findById(id);
            if (maybe.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            Video video = maybe.get();

            String path = video.getVideoUrl();
            if (!StringUtils.hasText(path)) path = video.getSourceUrl();
            if (!StringUtils.hasText(path)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // Don't proxy YouTube - instruct client to embed
            if (isYouTubeHost(path)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(output -> output.write("YouTube videos should be embedded on the client.".getBytes()));
            }

            // Remote URL -> proxy (with Range forwarding)
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return proxyRemoteVideo(path, rangeHeader);
            }

            // Local file streaming with Range support
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            long fileLength = file.length();
            long from = 0;
            long to = fileLength - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String rangeValue = rangeHeader.substring("bytes=".length());
                String[] parts = rangeValue.split("-", 2);
                try {
                    if (parts.length > 0 && parts[0].length() > 0) from = Long.parseLong(parts[0]);
                    if (parts.length == 2 && parts[1].length() > 0) to = Long.parseLong(parts[1]);
                    else {
                        long maxEnd = from + CHUNK_SIZE - 1;
                        if (maxEnd < to) to = maxEnd;
                    }
                } catch (NumberFormatException ex) {
                    from = 0; to = fileLength - 1;
                }
                if (from > to || from < 0 || to >= fileLength) {
                    HttpHeaders errHeaders = new HttpHeaders();
                    errHeaders.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength);
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(errHeaders).body(null);
                }
            }

            final long start = from;
            final long end = to;
            final RandomAccessFile raf = new RandomAccessFile(file, "r");

            StreamingResponseBody body = output -> {
                try (RandomAccessFile r = raf; OutputStream out = output) {
                    r.seek(start);
                    byte[] buffer = new byte[8192];
                    long toRead = end - start + 1;
                    while (toRead > 0) {
                        int len = r.read(buffer, 0, (int) Math.min(buffer.length, toRead));
                        if (len == -1) break;
                        out.write(buffer, 0, len);
                        toRead -= len;
                    }
                    out.flush();
                } catch (IOException ioe) {
                    // ignore (client aborted)
                }
            };

            String contentType = video.getContentType();
            if (!StringUtils.hasText(contentType)) {
                try {
                    String probe = Files.probeContentType(file.toPath());
                    contentType = probe == null ? "application/octet-stream" : probe;
                } catch (Exception ex) {
                    contentType = "application/octet-stream";
                }
            }

            HttpHeaders headers = buildCommonHeaders(
                    (video.getTitle() != null ? video.getTitle() : "video"),
                    contentType
            );
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(end - start + 1);

            if (rangeHeader != null) {
                headers.set(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileLength));
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers).body(body);
            } else {
                return ResponseEntity.ok().headers(headers).body(body);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private ResponseEntity<StreamingResponseBody> proxyRemoteVideo(String remoteUrl, String incomingRange) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(remoteUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            conn.setInstanceFollowRedirects(true);

            if (incomingRange != null && incomingRange.startsWith("bytes=")) {
                conn.setRequestProperty("Range", incomingRange);
            }
            conn.setRequestProperty("User-Agent", "dtao-lms-proxy/1.0");
            conn.connect();

            int remoteStatus = conn.getResponseCode();
            String contentType = conn.getContentType();
            if (!StringUtils.hasText(contentType)) contentType = "application/octet-stream";

            // Make InputStream final for lambda capture
            final InputStream remoteIn;
            try {
                remoteIn = conn.getInputStream();
            } catch (IOException ioe) {
                InputStream errStream = conn.getErrorStream();
                if (errStream != null) {
                    final InputStream errIn = errStream;
                    StreamingResponseBody bodyErr = output -> {
                        try (InputStream in = errIn; OutputStream out = output) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                        } catch (IOException ignored) {}
                    };
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(bodyErr);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
                }
            }

            final String remoteContentRange = conn.getHeaderField("Content-Range");
            final String cl = conn.getHeaderField("Content-Length");
            final long remoteContentLength;
            long tmpCl = -1;
            try { if (cl != null) tmpCl = Long.parseLong(cl); } catch (Exception ignored) {}
            remoteContentLength = tmpCl;

            StreamingResponseBody body = output -> {
                try (InputStream in = remoteIn; OutputStream out = output) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                } catch (IOException e) {
                    // ignore
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
            headers.set(HttpHeaders.CACHE_CONTROL, "no-store");
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"video\"");
            headers.set(HttpHeaders.VARY, List.of("Origin").stream().collect(Collectors.joining(",")));
            headers.set("X-Proxy-By", "dtao-lms");
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

            if (remoteContentRange != null) {
                headers.set(HttpHeaders.CONTENT_RANGE, remoteContentRange);
                return ResponseEntity.status(remoteStatus).headers(headers).body(body);
            }

            if (remoteContentLength >= 0) {
                headers.setContentLength(remoteContentLength);
            }

            if (remoteStatus == HttpURLConnection.HTTP_PARTIAL) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers).body(body);
            }
            return ResponseEntity.ok().headers(headers).body(body);

        } catch (Exception ex) {
            ex.printStackTrace();
            if (conn != null) conn.disconnect();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
        }
    }

    private HttpHeaders buildCommonHeaders(String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (filename == null ? "video" : filename) + "\"");
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Access-Control-Expose-Headers", "Content-Range, Accept-Ranges, Content-Length");
        return headers;
    }
}
