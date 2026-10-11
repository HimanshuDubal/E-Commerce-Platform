package com.ecommerce.category;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.common.BaseEntity;
import com.ecommerce.product.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Category extends BaseEntity{

	@Column(nullable = false, unique = true)
	private String name;

	private String description;

	    // Ignored in JSON: a Category response shouldn't drag every Product in that
	    // category with it (and each Product -> Category -> products would recurse).
	    @OneToMany(mappedBy = "category")
	    @JsonIgnore
	private List<Product> products = new ArrayList<>();
	public Category() {
		// TODO Auto-generated constructor stub
	}
	public Category(String name, String description, List<Product> products) {
		super();
		this.name = name;
		this.description = description;
		this.products = products;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Product> getProducts() {
		return products;
	}
	public void setProducts(List<Product> products) {
		this.products = products;
	}

	
}
