package com.ecommerce.wishlist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import com.ecommerce.dto.response.WishlistResponseDTO;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

	@Autowired
	private WishlistService wishlistService;
	@Autowired
	private AuthService authService;
	
	@GetMapping
    public ResponseEntity<WishlistResponseDTO> getWishlist(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(WishlistResponseDTO.from(wishlistService.getWishlistForUser(user)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponseDTO> addProduct(@PathVariable Long productId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(WishlistResponseDTO.from(wishlistService.addProduct(user, productId)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeProduct(@PathVariable Long productId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        wishlistService.removeProduct(user, productId);
        return ResponseEntity.noContent().build();
    }

}
