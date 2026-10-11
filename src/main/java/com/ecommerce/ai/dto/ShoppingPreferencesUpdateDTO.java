package com.ecommerce.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record ShoppingPreferencesUpdateDTO(
        @NotNull Boolean personalizationEnabled,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal minBudget,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal maxBudget,
        @Size(max = 25) List<@NotBlank @Size(max = 100) String> preferredCategories,
        @Size(max = 25) List<@NotBlank @Size(max = 100) String> preferredKeywords
) {
}
