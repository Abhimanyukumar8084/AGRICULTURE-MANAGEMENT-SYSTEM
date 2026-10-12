package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ConsultationRequest(
        UUID expertId,
        @NotBlank @Size(max = 100) String topic,
        @NotBlank @Size(max = 100) String crop,
        @NotBlank @Size(min = 10, max = 3000) String question) {
}
