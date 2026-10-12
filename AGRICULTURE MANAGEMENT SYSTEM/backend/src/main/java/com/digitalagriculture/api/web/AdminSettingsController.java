package com.digitalagriculture.api.web;

import com.digitalagriculture.api.config.PlatformSetting;
import com.digitalagriculture.api.config.PlatformSettingRepository;
import com.digitalagriculture.api.web.dto.PlatformSettingsRequest;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {
    private final PlatformSettingRepository settings;

    public AdminSettingsController(PlatformSettingRepository settings) {
        this.settings = settings;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, String> get() {
        return settings.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(PlatformSetting::getKey, PlatformSetting::getValue));
    }

    @PutMapping
    @Transactional
    public Map<String, String> update(@Valid @RequestBody PlatformSettingsRequest request) {
        try {
            ZoneId.of(request.timeZone());
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid time zone.");
        }
        Map<String, String> updates = Map.of(
                "platform_name", request.platformName().trim(),
                "default_language", request.defaultLanguage(),
                "time_zone", request.timeZone(),
                "notify_new_farmer", Boolean.toString(request.notifyNewFarmer()),
                "notify_feedback", Boolean.toString(request.notifyFeedback()),
                "notify_market_prices", Boolean.toString(request.notifyMarketPrices()));
        Map<String, PlatformSetting> stored = settings.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, Function.identity()));
        updates.forEach((key, value) -> {
            PlatformSetting setting = stored.get(key);
            if (setting == null) throw new IllegalStateException("Required platform setting is missing: " + key);
            setting.setValue(value);
        });
        settings.saveAll(stored.values());
        return get();
    }
}
