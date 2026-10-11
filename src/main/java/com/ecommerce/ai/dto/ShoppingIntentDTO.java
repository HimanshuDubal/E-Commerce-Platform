package com.ecommerce.ai.dto;

import java.math.BigDecimal;
import java.util.List;

/** Structured needs extracted from the shopper's natural-language request by the model. */
public record ShoppingIntentDTO(
        String productType,
        String category,
        String useCase,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        List<String> requiredFeatures,
        List<String> preferences,
        List<String> searchKeywords
) {
}
