package com.ecommerce.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.ecommerce.auth.User;
import com.ecommerce.order.Order;
import com.ecommerce.order.OrderItem;
import com.ecommerce.order.OrderStatus;

@Component
public class OrderLifecycleEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderLifecycleEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void orderPlaced(Order order) {
        publish(order, OrderLifecycleEvent.Type.ORDER_PLACED, null);
    }

    public void statusChanged(Order order, OrderStatus previousStatus) {
        OrderLifecycleEvent.Type type = order.getStatus() == OrderStatus.CANCELLED
                ? OrderLifecycleEvent.Type.ORDER_CANCELLED
                : OrderLifecycleEvent.Type.ORDER_STATUS_CHANGED;
        publish(order, type, previousStatus == null ? null : previousStatus.name());
    }

    private void publish(Order order, OrderLifecycleEvent.Type type, String previousStatus) {
        User user = order.getUser();
        List<OrderLifecycleEvent.InvoiceLine> lines = order.getItems() == null ? List.of()
                : order.getItems().stream().map(this::toInvoiceLine).toList();

        OrderLifecycleEvent event = new OrderLifecycleEvent(
                type,
                order.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                previousStatus,
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount(),
                order.getShippingAddress(),
                lines,
                LocalDateTime.now());
        applicationEventPublisher.publishEvent(event);
    }

    private OrderLifecycleEvent.InvoiceLine toInvoiceLine(OrderItem item) {
        String productName = item.getProduct() == null ? "Product" : item.getProduct().getName();
        BigDecimal unitPrice = item.getPriceAtPurchase() == null ? BigDecimal.ZERO : item.getPriceAtPurchase();
        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        return new OrderLifecycleEvent.InvoiceLine(
                productName,
                quantity,
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }
}
