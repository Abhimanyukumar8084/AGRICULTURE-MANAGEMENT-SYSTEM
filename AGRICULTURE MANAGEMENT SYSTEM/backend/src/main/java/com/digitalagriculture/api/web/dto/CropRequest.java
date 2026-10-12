package com.digitalagriculture.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record CropRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 32) String type,
        @NotBlank @Size(max = 24) String season,
        @Size(max = 100) String variety,
        @Size(max = 80) String growingDuration,
        @Size(max = 16) String waterRequirement,
        @Size(max = 60) String soilType,
        @Size(max = 80) String temperatureRange,
        @Size(max = 2000) String description,
        @DecimalMin(value = "0.01", message = "Average price must be greater than zero") BigDecimal averagePrice,
        @Size(max = 24) String priceUnit,
        @DecimalMin(value = "0.01", message = "MSP must be greater than zero") BigDecimal msp,
        @Size(max = 16) String status) {
}
