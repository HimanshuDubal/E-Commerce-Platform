package com.ecommerce.coupon;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import com.ecommerce.dto.CouponApplyRequest;
import com.ecommerce.dto.response.CouponResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

	@Autowired
	private CouponService couponService;
	@Autowired
	private AuthService authService;
	
	@PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponseDTO> createCoupon(@Valid @RequestBody Coupon coupon) {
		Coupon created = couponService.createCoupon(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponseDTO.from(created));
    }

    @PostMapping("/apply")
    public ResponseEntity<BigDecimal> applyCoupon(@Valid @RequestBody CouponApplyRequest request,
                                                  Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        BigDecimal discount = couponService.applyCoupon(user, request.getCode());
        return ResponseEntity.ok(discount);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponseDTO>> getAllCoupons() {
    		List<CouponResponseDTO> body = couponService.getAllCoupons().stream()
                .map(CouponResponseDTO::from)
                .toList();
        return ResponseEntity.ok(body);
    }

}
