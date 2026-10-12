package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Pattern(regexp = "^$|^[+0-9() .-]{7,24}$", message = "Enter a valid phone number") String phone,
        @Size(max = 120) String location,
        @Size(max = 80) String state,
        @Size(max = 80) String district,
        @Size(max = 120) String farmName,
        @Size(max = 40) String farmArea,
        @Size(max = 60) String soilType,
        @Size(max = 60) String irrigationMethod,
        Boolean notifyWeather,
        Boolean notifyMarket,
        Boolean notifyActivities) {
}
