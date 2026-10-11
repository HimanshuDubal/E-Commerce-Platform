package com.ecommerce.ai.dto;

import java.math.BigDecimal;
import java.util.List;

/** Only catalog facts are returned; no JPA entity or relationship is serialized. */
public record ProductRecommendationDTO(
        Long productId,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        String categoryName,
        int matchScore,
        List<String> matchedRequirements,
        List<String> unverifiedRequirements,
        String reason
) {
}
