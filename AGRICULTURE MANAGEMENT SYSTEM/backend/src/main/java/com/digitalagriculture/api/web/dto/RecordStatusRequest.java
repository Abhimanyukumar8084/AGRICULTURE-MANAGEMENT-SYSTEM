package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecordStatusRequest(@NotBlank @Size(max = 16) String status) {
}
