package com.digitalagriculture.api.web.dto;

import com.digitalagriculture.api.consultation.ConsultationStatus;
import jakarta.validation.constraints.NotNull;

public record ConsultationStatusRequest(@NotNull ConsultationStatus status) {
}
