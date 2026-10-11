package com.ecommerce.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import com.ecommerce.dto.response.ReviewResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

	@Autowired
	private ReviewService reviewService;
	@Autowired
	private AuthService authService;
	
	@GetMapping
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsForProduct(@PathVariable Long productId, Pageable pageable) {
		Page<ReviewResponseDTO> body = reviewService.getReviewsForProduct(productId, pageable)
                .map(ReviewResponseDTO::from);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getAverageRating(productId));
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> addReview(@PathVariable Long productId,
                            @Valid @RequestBody Review review,
                            Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        Review created = reviewService.addReview(productId, user, review);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponseDTO.from(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long productId,
                                          @PathVariable Long id,
                                          Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        reviewService.deleteReview(productId, id, user);
        return ResponseEntity.noContent().build();
    }

}
