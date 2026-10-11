package com.ecommerce.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.Role;
import com.ecommerce.auth.User;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.common.exceptions.UnauthorizedException;
import com.ecommerce.order.OrderRepository;
import com.ecommerce.order.OrderStatus;
import com.ecommerce.product.Product;
import com.ecommerce.product.ProductRepository;

@Service
public class ReviewService {

	@Autowired
	private ReviewRepository reviewRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private OrderRepository orderRepository;

    public Page<Review> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable);
    }

    @Cacheable(value = "productRatings", key = "#productId")
    public Double getAverageRating(Long productId) {
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        return avg == null ? 0.0 : avg;
    }

    @Transactional
    @CacheEvict(value = "productRatings", key = "#productId")
    public Review addReview(Long productId, User user, Review review) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean hasDeliveredOrder = orderRepository.findByUserId(user.getId(), Pageable.unpaged())
                .stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.DELIVERED &&
                        order.getItems().stream().anyMatch(item -> item.getProduct().getId().equals(productId)));
        if (!hasDeliveredOrder) {
            throw new IllegalStateException("You can only review products from delivered orders");
        }
        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        review.setProduct(product);
        review.setUser(user);
        return reviewRepository.save(review);
    }

    @Transactional
    @CacheEvict(value = "productRatings", key = "#productId")
    public void deleteReview(Long productId, Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Not authorized to delete this review");
        }
        reviewRepository.delete(review);
    }

}
