package com.ecommerce.order;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.Role;
import com.ecommerce.auth.User;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.common.exceptions.UnauthorizedException;
import com.ecommerce.dto.OrderStatusRequestUpdate;
import com.ecommerce.dto.response.OrderResponseDTO;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private AuthService authService;
	
	@PostMapping("/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(@RequestParam String shippingAddress,@RequestParam(required = false)String couponCode, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        Order order = orderService.createOrderFromCart(user, shippingAddress, couponCode);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/orders/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(location).body(OrderResponseDTO.from(order));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(Authentication authentication, Pageable pageable) {
        User user = authService.getCurrentUser(authentication);
        Page<OrderResponseDTO> body = orderRepository.findByUserId(user.getId(), pageable)
                .map(OrderResponseDTO::from);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderDetail(@PathVariable Long id, Authentication authentication) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        User user = authService.getCurrentUser(authentication);
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Not authorized");
        }
        return ResponseEntity.ok(OrderResponseDTO.from(order));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<OrderResponseDTO> updateStatus(@PathVariable Long id, @RequestBody OrderStatusRequestUpdate request, Authentication authentication) {
    	User currentUser = authService.getCurrentUser(authentication);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (currentUser.getRole() == Role.SELLER) {
            boolean ownsAnItem = order.getItems().stream()
                    .anyMatch(item -> item.getProduct().getSeller() != null
                            && item.getProduct().getSeller().getId().equals(currentUser.getId()));
            if (!ownsAnItem) {
                throw new UnauthorizedException("You do not sell any product in this order");
            }
        }
        Order updated = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(OrderResponseDTO.from(updated));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        Order cancelled = orderService.cancelOrder(id, user);
        return ResponseEntity.ok(OrderResponseDTO.from(cancelled));
    }

}
