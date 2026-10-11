package com.ecommerce.product;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.Role;
import com.ecommerce.auth.User;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.common.exceptions.UnauthorizedException;

@Service
public class ProductService {

	@Autowired
	private ProductRepository productRepository;

    public Page<Product> searchProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                         String keyword, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasCategory(categoryId))
                .and(ProductSpecification.priceBetween(minPrice, maxPrice))
                .and(ProductSpecification.nameContains(keyword));
        return productRepository.findAll(spec, pageable);
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Transactional
    public Product createProduct(Product product, User seller) {
        product.setSeller(seller);
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public Product updateProduct(Long id, Product updated, User currentUser) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        assertOwnerOrAdmin(existing, currentUser);

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setStock(updated.getStock());
        existing.setImageUrl(updated.getImageUrl());
        if (updated.getCategory() != null) {
            existing.setCategory(updated.getCategory());
        }
        return productRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id, User currentUser) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        assertOwnerOrAdmin(product, currentUser);
        productRepository.delete(product);
    }

    @Transactional
    public void bulkImport(List<Product> products, User seller) {
        products.forEach(p -> p.setSeller(seller));
        productRepository.saveAll(products);
    }

    private void assertOwnerOrAdmin(Product product, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && !product.getSeller().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not the seller of this product");
        }
    }

}
