package com.ecommerce.wishlist;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.auth.User;
import com.ecommerce.common.BaseEntity;
import com.ecommerce.product.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "wishlists")
public class Wishlist extends BaseEntity{

	 @OneToOne(fetch = FetchType.LAZY)
	 @JoinColumn(name = "user_id", unique = true)
	 @JsonIgnore
	 private User user;

	 @ManyToMany
	 @JoinTable(
	            name = "wishlist_products",
	            joinColumns = @JoinColumn(name = "wishlist_id"),
	            inverseJoinColumns = @JoinColumn(name = "product_id")
	    )
	private List<Product> products = new ArrayList<>();
	
	public Wishlist() {
		// TODO Auto-generated constructor stub
	}

	public Wishlist(User user, List<Product> products) {
		super();
		this.user = user;
		this.products = products;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}

}
