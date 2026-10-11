package com.ecommerce.wishlist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.User;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.product.Product;
import com.ecommerce.product.ProductRepository;

@Service
public class WishlistService {

	@Autowired
	private WishlistRepository wishlistRepository;
    @Autowired
	private ProductRepository productRepository;

    public Wishlist getWishlistForUser(User user) {
        return wishlistRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));
    }

    @Transactional
    public Wishlist addProduct(User user, Long productId) {
        Wishlist wishlist = getWishlistForUser(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!wishlist.getProducts().contains(product)) {
            wishlist.getProducts().add(product);
            wishlistRepository.save(wishlist);
        }
        return wishlist;
    }

    @Transactional
    public void removeProduct(User user, Long productId) {
        Wishlist wishlist = getWishlistForUser(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        wishlist.getProducts().remove(product);
        wishlistRepository.save(wishlist);
    }

}
