package com.digitalagriculture.api.web.dto;

import com.digitalagriculture.api.record.RecordKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FarmRecordRequest(
        @NotNull RecordKind kind,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 200) String category,
        @Size(max = 3000) String description,
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero") BigDecimal quantity,
        @Size(max = 32) String unit,
        @DecimalMin(value = "0.01", message = "Price must be greater than zero") BigDecimal price,
        @Size(max = 120) String location,
        @Size(max = 16) String status,
        LocalDate dueDate) {
}
