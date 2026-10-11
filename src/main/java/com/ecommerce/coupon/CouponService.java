package com.ecommerce.coupon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.auth.User;
import com.ecommerce.cart.Cart;
import com.ecommerce.cart.CartItem;
import com.ecommerce.cart.CartRepository;
import com.ecommerce.common.exceptions.ResourceNotFoundException;

@Service
public class CouponService {

	@Autowired
	private CouponRepository couponRepository;
    @Autowired
	private CartRepository cartRepository;

    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public BigDecimal applyCoupon(User user, String code) {
        Coupon coupon = couponRepository.findByCodeAndExpiryDateAfter(code, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon invalid or expired"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        BigDecimal cartTotal = lineTotal(cart.getItems());

        if (coupon.getMinCartValue() != null && cartTotal.compareTo(coupon.getMinCartValue()) < 0) {
            throw new IllegalStateException(
                    "Cart total does not meet the minimum of " + coupon.getMinCartValue() + " for this coupon");
        }

        BigDecimal eligibleTotal = cartTotal;
        if (!coupon.getApplicableProducts().isEmpty()) {
            Set<Long> applicableIds = coupon.getApplicableProducts().stream()
                    .map(com.ecommerce.product.Product::getId)
                    .collect(Collectors.toSet());
            List<CartItem> eligibleItems = cart.getItems().stream()
                    .filter(item -> applicableIds.contains(item.getProduct().getId()))
                    .collect(Collectors.toList());
            if (eligibleItems.isEmpty()) {
                throw new IllegalStateException("Coupon does not apply to any item currently in your cart");
            }
            eligibleTotal = lineTotal(eligibleItems);
        }

        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            return eligibleTotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
        }
        // FLAT discount can never exceed what it's being applied against
        return coupon.getDiscountValue().min(eligibleTotal);
    }

    private BigDecimal lineTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
