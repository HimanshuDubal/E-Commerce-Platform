package com.ecommerce.coupon;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>{

	Optional<Coupon> findByCodeAndExpiryDateAfter(String code, LocalDate date);
}
