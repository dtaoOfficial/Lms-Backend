package com.dtao.lms.service;

import com.dtao.lms.model.SystemSetting;
import com.dtao.lms.repo.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    public SystemSetting getSettings() {
        return systemSettingRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> systemSettingRepository.save(new SystemSetting()));
    }

    public SystemSetting updateSettings(SystemSetting newSettings) {
        SystemSetting current = getSettings();

        current.setAutoApproveCourses(newSettings.isAutoApproveCourses());
        current.setMaintenanceMode(newSettings.isMaintenanceMode());
        current.setAllowRegistration(newSettings.isAllowRegistration());
        current.setDefaultUserRole(newSettings.getDefaultUserRole());
        current.setThemeMode(newSettings.getThemeMode());

        return systemSettingRepository.save(current);
    }

    public SystemSetting resetSettings() {
        systemSettingRepository.deleteAll();
        return systemSettingRepository.save(new SystemSetting());
    }
}
