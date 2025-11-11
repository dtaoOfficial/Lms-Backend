package com.dtao.lms.controller;

import com.dtao.lms.model.SystemSetting;
import com.dtao.lms.service.SystemSettingService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class AdminSettingsController {

    @Autowired
    private SystemSettingService systemSettingService;

    @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * ✅ Get current system settings
     */
    @GetMapping
    public ResponseEntity<SystemSetting> getSettings() {
        SystemSetting settings = systemSettingService.getSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * 🛠️ Update system settings (admin only)
     */
    @PatchMapping("/update")
    public ResponseEntity<SystemSetting> updateSettings(@RequestBody SystemSetting settings) {
        SystemSetting updated = systemSettingService.updateSettings(settings);
        return ResponseEntity.ok(updated);
    }

    /**
     * 🔄 Reset settings to defaults
     */
    @PostMapping("/reset")
    public ResponseEntity<SystemSetting> resetSettings() {
        SystemSetting reset = systemSettingService.resetSettings();
        return ResponseEntity.ok(reset);
    }

    // ✅ Optional debug: log allowed origins at startup (useful in Render logs)
    @PostConstruct
    public void logAllowedOrigins() {
        System.out.println("🌍 [AdminSettingsController] Allowed origins: " + allowedOrigins);
    }
}
