package com.ecommerce.coupon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ecommerce.common.BaseEntity;
import com.ecommerce.product.Product;

import jakarta.persistence.*;

@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity{

	 @Column(unique = true)
	    private String code;

	    @Enumerated(EnumType.STRING)
	    private DiscountType discountType;

	    private BigDecimal discountValue;

	    private LocalDate expiryDate;

	    private BigDecimal minCartValue;

	    @ManyToMany
	    @JoinTable(
	            name = "coupon_products",
	            joinColumns = @JoinColumn(name = "coupon_id"),
	            inverseJoinColumns = @JoinColumn(name = "product_id")
	    )
	    private List<Product> applicableProducts = new ArrayList<>();

	    public enum DiscountType {
	        PERCENTAGE, FLAT
	    }
	    
	    public Coupon(String code, DiscountType discountType, BigDecimal discountValue, LocalDate expiryDate,
				BigDecimal minCartValue, List<Product> applicableProducts) {
			super();
			this.code = code;
			this.discountType = discountType;
			this.discountValue = discountValue;
			this.expiryDate = expiryDate;
			this.minCartValue = minCartValue;
			this.applicableProducts = applicableProducts;
		}


		public String getCode() {
			return code;
		}


		public void setCode(String code) {
			this.code = code;
		}


		public DiscountType getDiscountType() {
			return discountType;
		}


		public void setDiscountType(DiscountType discountType) {
			this.discountType = discountType;
		}


		public BigDecimal getDiscountValue() {
			return discountValue;
		}


		public void setDiscountValue(BigDecimal discountValue) {
			this.discountValue = discountValue;
		}


		public LocalDate getExpiryDate() {
			return expiryDate;
		}


		public void setExpiryDate(LocalDate expiryDate) {
			this.expiryDate = expiryDate;
		}


		public BigDecimal getMinCartValue() {
			return minCartValue;
		}


		public void setMinCartValue(BigDecimal minCartValue) {
			this.minCartValue = minCartValue;
		}


		public List<Product> getApplicableProducts() {
			return applicableProducts;
		}


		public void setApplicableProducts(List<Product> applicableProducts) {
			this.applicableProducts = applicableProducts;
		}


		public Coupon() {
		// TODO Auto-generated constructor stub
		}

}
