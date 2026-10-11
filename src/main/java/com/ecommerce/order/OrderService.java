package com.ecommerce.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.Role;
import com.ecommerce.auth.User;
import com.ecommerce.cart.Cart;
import com.ecommerce.cart.CartItem;
import com.ecommerce.cart.CartItemRepository;
import com.ecommerce.cart.CartRepository;
import com.ecommerce.common.exceptions.InvalidStateTransitionException;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.common.exceptions.UnauthorizedException;
import com.ecommerce.coupon.CouponService;
import com.ecommerce.notification.event.OrderLifecycleEventPublisher;
import com.ecommerce.product.Product;
import com.ecommerce.product.ProductRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired 
    private CartItemRepository cartItemRepository;
    @Autowired 
    private ProductRepository productRepository;
    @Autowired 
    private CouponService couponService;
    @Autowired 
    private CacheManager cacheManager;
    @Autowired 
    private OrderLifecycleEventPublisher orderEventPublisher;

    @Transactional
    public Order createOrderFromCart(User user, String shippingAddress, String couponCode) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
            evictProductCache(product.getId());
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.PLACED);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtPurchase(cartItem.getProduct().getPrice());
            orderItems.add(item);
            total = total.add(cartItem.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setItems(orderItems);

        // Always compute discount server-side from the coupon code.
        if (couponCode != null && !couponCode.isBlank()) {
            BigDecimal discount = couponService.applyCoupon(user, couponCode);
            total = total.subtract(discount);
            if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
        }
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteByCartId(cart.getId());

        // Listener runs only after commit; the event contains immutable values, not JPA entities.
        orderEventPublisher.orderPlaced(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus previousStatus = order.getStatus();
        if (!previousStatus.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition from " + previousStatus + " to " + newStatus);
        }
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.statusChanged(savedOrder, previousStatus);
        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Not authorized to cancel this order");
        }
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order cannot be cancelled in its current state");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
            evictProductCache(product.getId());
        }
        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.statusChanged(savedOrder, previousStatus);
        return savedOrder;
    }

    private void evictProductCache(Long productId) {
        Cache productCache = cacheManager.getCache("products");
        if (productCache != null) productCache.evict(productId);
    }
}
