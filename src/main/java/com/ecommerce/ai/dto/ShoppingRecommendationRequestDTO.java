package com.ecommerce.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShoppingRecommendationRequestDTO(
        @NotBlank(message = "Please describe what you want to buy")
        @Size(max = 1000, message = "Shopping request must be at most 1000 characters")
        String message
) {
}
