package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlatformSettingsRequest(
        @NotBlank @Size(max = 80) String platformName,
        @NotBlank @Pattern(regexp = "^(English|Hindi)$") String defaultLanguage,
        @NotBlank @Pattern(regexp = "^[A-Za-z_+-]+(?:/[A-Za-z0-9_+-]+)*$") String timeZone,
        boolean notifyNewFarmer,
        boolean notifyFeedback,
        boolean notifyMarketPrices) {
}
