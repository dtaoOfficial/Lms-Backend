package com.dtao.lms.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CertificateService
 *
 * - generates a simple PDF certificate placed into `certificates` folder
 * - keeps minimal metadata in memory (for demo). In production store metadata in DB.
 */
@Service
public class CertificateService {

    private final Path storageDir;
    private final Map<String, Map<String,Object>> metadata = Collections.synchronizedMap(new LinkedHashMap<>());

    public CertificateService(@Value("${app.certificates.dir:certificates}") String dir) {
        this.storageDir = Paths.get(dir).toAbsolutePath();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create certificates dir: " + this.storageDir, e);
        }
    }

    /**
     * Generate certificate for user/course.
     * Returns metadata map with { id, downloadUrl, createdAt }
     */
    public Map<String, Object> generateCertificateForUser(String userEmail, String courseId, String courseTitle) throws IOException {
        // verify inputs
        if (userEmail == null || courseId == null) throw new IllegalArgumentException("userEmail and courseId required");

        String id = UUID.randomUUID().toString();
        String fileName = id + ".pdf";
        Path out = storageDir.resolve(fileName);

        // create a simple PDF (PDFBox)
        createSimpleCertificatePdf(out, userEmail, courseTitle == null ? ("Course " + courseId) : courseTitle);

        Map<String, Object> meta = new HashMap<>();
        meta.put("id", id);
        meta.put("fileName", fileName);
        meta.put("downloadUrl", "/api/certificates/" + id + "/download");
        meta.put("userEmail", userEmail);
        meta.put("courseId", courseId);
        meta.put("courseTitle", courseTitle);
        meta.put("createdAt", Instant.now().toString());

        metadata.put(id, meta);
        return meta;
    }

    /**
     * Return bytes for a stored certificate id or null if missing
     */
    public byte[] getCertificateBlob(String id) throws IOException {
        Map<String,Object> meta = metadata.get(id);
        if (meta == null) {
            // try to locate on disk by id
            Path p = storageDir.resolve(id + ".pdf");
            if (Files.exists(p)) return Files.readAllBytes(p);
            return null;
        }
        Path p = storageDir.resolve(String.valueOf(meta.get("fileName")));
        if (!Files.exists(p)) return null;
        return Files.readAllBytes(p);
    }

    /**
     * List certificates for a user (basic in-memory filter)
     * In production: query DB for certificates by user.
     */
    public List<Map<String,Object>> listCertificatesForUser(String userEmail) {
        return metadata.values().stream()
                .filter(m -> userEmail.equals(m.get("userEmail")))
                .map(HashMap::new)
                .collect(Collectors.toList());
    }

    /* ================= helper: create PDF with PDFBox ================== */

    private void createSimpleCertificatePdf(Path out, String userEmail, String courseTitle) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            // load a system font if available (fallback to built-in)
            // In some environments, loading TTF from system may fail — this is best-effort.
            PDType0Font font = null;
            try {
                // attempt to load an embedded font from resources (if you bundle one)
                // otherwise fallback to default Helvetica-like rendering via PDType0Font.load with system font not always available.
                font = PDType0Font.load(doc, new ByteArrayInputStream(new byte[0]));
            } catch (Exception ignored) {
                // leave font null and use default operations (text fallback)
            }

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // background
                cs.setNonStrokingColor(new Color(245, 247, 250));
                cs.addRect(0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
                cs.fill();

                // Title
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 28);
                cs.setNonStrokingColor(new Color(30, 41, 59)); // slate-800
                cs.newLineAtOffset(70, 520);
                cs.showText("Certificate of Completion");
                cs.endText();

                // Course title
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 18);
                cs.setNonStrokingColor(Color.DARK_GRAY);
                cs.newLineAtOffset(70, 480);
                cs.showText(courseTitle != null ? courseTitle : "Course");
                cs.endText();

                // Awarded to
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 14);
                cs.setNonStrokingColor(Color.DARK_GRAY);
                cs.newLineAtOffset(70, 430);
                cs.showText("Awarded to");
                cs.endText();

                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 20);
                cs.newLineAtOffset(70, 400);
                cs.showText(userEmail);
                cs.endText();

                // small footer
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE, 10);
                cs.setNonStrokingColor(Color.GRAY);
                cs.newLineAtOffset(70, 80);
                cs.showText("Generated by LMS - " + Instant.now().toString());
                cs.endText();
            }

            // save
            try (OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                doc.save(os);
            }
        }
    }

}
