package com.ecommerce.ai.dto;

import jakarta.validation.constraints.Size;

/** A blank or omitted message requests recommendations from saved preferences and on-site history alone. */
public record PersonalizedRecommendationRequestDTO(
        @Size(max = 1000, message = "Shopping request must be at most 1000 characters")
        String message
) {
}
