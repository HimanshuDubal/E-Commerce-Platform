package com.ecommerce.dto.response;

import com.ecommerce.review.Review;

import java.time.LocalDateTime;

public record ReviewResponseDTO(
        Long id,
        Long productId,
        String productName,
        Long userId,
        String reviewerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponseDTO from(Review review) {
        if (review == null) return null;
        return new ReviewResponseDTO(
                review.getId(),
                review.getProduct() == null ? null : review.getProduct().getId(),
                review.getProduct() == null ? null : review.getProduct().getName(),
                review.getUser() == null ? null : review.getUser().getId(),
                review.getUser() == null ? null : review.getUser().getName(),
                review.getRating(), review.getComment(), review.getCreatedAt(), review.getUpdatedAt()
        );
    }
}
