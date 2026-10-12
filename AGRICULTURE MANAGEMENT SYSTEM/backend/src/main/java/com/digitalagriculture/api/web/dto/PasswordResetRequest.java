package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 12, max = 72, message = "Password must contain between 12 and 72 characters") String password) {
}
