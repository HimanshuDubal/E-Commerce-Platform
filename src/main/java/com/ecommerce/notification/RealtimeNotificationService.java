package com.ecommerce.notification;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.notification.dto.NotificationDTO;
import com.ecommerce.notification.event.OrderLifecycleEvent;

@Service
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendOrderEvent(OrderLifecycleEvent event) {
        String title;
        String message;

        switch (event.type()) {
            case ORDER_PLACED -> {
                title = "Order placed";
                message = "Your order #" + event.orderId() + " has been placed successfully.";
            }
            case ORDER_CANCELLED -> {
                title = "Order cancelled";
                message = "Your order #" + event.orderId() + " has been cancelled.";
            }
            default -> {
                title = "Order status updated";
                message = "Your order #" + event.orderId() + " is now " + event.status() + ".";
            }
        }

        NotificationDTO notification = new NotificationDTO(
                event.type().name(),
                title,
                message,
                event.orderId(),
                event.status(),
                event.occurredAt());

        // User destinations resolve against the authenticated STOMP Principal (email).
        messagingTemplate.convertAndSendToUser(
                event.customerEmail(), "/queue/notifications", notification);
    }
}
