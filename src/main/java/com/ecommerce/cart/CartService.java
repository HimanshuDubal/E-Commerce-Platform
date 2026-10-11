package com.ecommerce.cart;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.User;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.common.exceptions.UnauthorizedException;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.product.Product;
import com.ecommerce.product.ProductRepository;

@Service
public class CartService {

	@Autowired
	private CartRepository cartRepository;
    @Autowired
	private CartItemRepository cartItemRepository;
    @Autowired
    private ProductRepository productRepository;
	
    public Cart getCartForUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    @Transactional
    public Cart addItem(User user, CartItemRequest request) {
    	if(request.getQuantity() <= 0) {
    		throw new IllegalArgumentException("Quantity must be greater than Zero");
    	}
        Cart cart = getCartForUser(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Optional<CartItem>existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        int currentQuantityInCart = existing.map(CartItem::getQuantity).orElse(0);
        int totalRequestedQuantity = currentQuantityInCart + request.getQuantity();
        
        if(totalRequestedQuantity > product.getStock()) {
        	throw new IllegalArgumentException("Requested quantity (" + totalRequestedQuantity + ") exceeds available stock (" + product.getStock() + ")");
        }
        
        if (existing.isPresent()) {
        	CartItem item = existing.get();
            item.setQuantity(totalRequestedQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
            cartRepository.save(cart);
        }
        return getCartForUser(user);
    }

    @Transactional
    public Cart updateItemQuantity(User user, Long itemId, CartItemRequest request) {
        Cart cart = getCartForUser(user);
        CartItem item = getOwnedItem(cart, itemId);
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return getCartForUser(user);
    }

    @Transactional
    public void removeItem(User user, Long itemId) {
        Cart cart = getCartForUser(user);
        CartItem item = getOwnedItem(cart, itemId);
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getCartForUser(user);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private CartItem getOwnedItem(Cart cart, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new UnauthorizedException("Item does not belong to your cart");
        }
        return item;
    }

}
