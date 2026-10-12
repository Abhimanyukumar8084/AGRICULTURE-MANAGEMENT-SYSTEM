package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminFarmerRequest(
        @NotBlank @Size(max = 161) String fullName,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Enter a 10-digit mobile number") String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,40}$", message = "Username must be 3-40 letters, numbers, dots, underscores or hyphens") String username,
        @NotBlank @Size(min = 12, max = 72, message = "Password must contain between 12 and 72 characters") String password,
        @Size(max = 16) String gender,
        @NotBlank @Size(max = 80) String state,
        @NotBlank @Size(max = 80) String district,
        @Size(max = 120) String village,
        @Pattern(regexp = "^$|^[0-9]{6}$", message = "Enter a 6-digit PIN code") String pinCode,
        @NotBlank @Size(max = 80) String mainCrop,
        @Size(max = 40) String farmingType,
        @DecimalMin(value = "0.01", message = "Land area must be greater than zero") BigDecimal landArea,
        @Size(max = 24) String landUnit,
        @NotBlank @Pattern(regexp = "^(ACTIVE|PENDING|INACTIVE)$", message = "Choose a valid account status") String status) {
}
