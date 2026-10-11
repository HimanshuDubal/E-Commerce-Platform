package com.ecommerce.order;

import java.math.BigDecimal;

import com.ecommerce.common.BaseEntity;
import com.ecommerce.product.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity{

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    // Snapshot of price at purchase time - must NOT change if the product's
    // price changes later.
    private BigDecimal priceAtPurchase;
    
	public OrderItem() {
		// TODO Auto-generated constructor stub
	}

	public OrderItem(Order order, Product product, Integer quantity, BigDecimal priceAtPurchase) {
		super();
		this.order = order;
		this.product = product;
		this.quantity = quantity;
		this.priceAtPurchase = priceAtPurchase;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getPriceAtPurchase() {
		return priceAtPurchase;
	}

	public void setPriceAtPurchase(BigDecimal priceAtPurchase) {
		this.priceAtPurchase = priceAtPurchase;
	}
	
	

}
