package com.ecommerce.ai.dto;

import java.util.List;

public record ShoppingRecommendationResponseDTO(
        String assistantMessage,
        ShoppingIntentDTO interpretedNeeds,
        List<ProductRecommendationDTO> recommendations
) {
}
