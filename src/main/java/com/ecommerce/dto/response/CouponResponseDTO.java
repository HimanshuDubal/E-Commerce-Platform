package com.ecommerce.dto.response;

import com.ecommerce.coupon.Coupon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CouponResponseDTO(
		Long id,
        String code,
        String discountType,
        BigDecimal discountValue,
        LocalDate expiryDate,
        BigDecimal minCartValue,
        List<Long> applicableProductIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CouponResponseDTO from(Coupon coupon) {
        if (coupon == null) return null;
        List<Long> productIds = coupon.getApplicableProducts() == null ? List.of()
                : coupon.getApplicableProducts().stream().map(product -> product.getId()).toList();
        return new CouponResponseDTO(
        		coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType() == null ? null : coupon.getDiscountType().name(),
                coupon.getDiscountValue(), coupon.getExpiryDate(), coupon.getMinCartValue(),
                productIds, coupon.getCreatedAt(), coupon.getUpdatedAt()
        );
    }
}
