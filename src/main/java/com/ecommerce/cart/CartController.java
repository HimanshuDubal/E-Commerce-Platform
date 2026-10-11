package com.ecommerce.cart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.response.CartResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

	@Autowired
	private CartService cartService;
	@Autowired
	private AuthService authService;
	
	@GetMapping
    public ResponseEntity<CartResponseDTO> getCart(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(CartResponseDTO.from(cartService.getCartForUser(user)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(@Valid @RequestBody CartItemRequest request, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(CartResponseDTO.from(cartService.addItem(user, request)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> updateQuantity(@PathVariable Long itemId,
                               @Valid @RequestBody CartItemRequest request,
                               Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(CartResponseDTO.from(cartService.updateItemQuantity(user, itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable Long itemId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        cartService.removeItem(user, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }

}
