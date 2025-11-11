package com.dtao.lms.controller;

import com.dtao.lms.model.XpEvent;
import com.dtao.lms.service.XpEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/xp")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class XpEventController {

    @Autowired
    private XpEventService xpEventService;

    /**
     * ✅ Fetch current user's XP total and recent history
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyXp(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        String email = principal.getName();

        int totalXp = xpEventService.getTotalXp(email);
        List<XpEvent> history = xpEventService.getXpHistory(email);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", email);
        result.put("totalXp", totalXp);
        result.put("events", history);

        return ResponseEntity.ok(result);
    }

    /**
     * ✅ Fetch XP history for any student (admin use)
     */
    @GetMapping("/user/{email}")
    public ResponseEntity<?> getXpByUser(@PathVariable String email) {
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Email required"));
        List<XpEvent> list = xpEventService.getXpHistory(email);
        return ResponseEntity.ok(list);
    }
}
