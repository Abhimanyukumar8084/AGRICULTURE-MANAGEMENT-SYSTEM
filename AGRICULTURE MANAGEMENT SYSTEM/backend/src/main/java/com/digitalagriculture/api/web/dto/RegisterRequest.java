package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^[+0-9() .-]{7,24}$", message = "Enter a valid phone number") String phone,
        @NotBlank @Size(min = 12, max = 72, message = "Password must contain between 12 and 72 characters") String password,
        @NotBlank String accountType) {
}
