package com.ecommerce.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShoppingPreferencesResponseDTO(
        boolean personalizationEnabled,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        List<String> preferredCategories,
        List<String> preferredKeywords
) {
}
