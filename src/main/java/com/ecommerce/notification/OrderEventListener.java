package com.ecommerce.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ecommerce.notification.event.OrderLifecycleEvent;
import com.ecommerce.service.EmailService;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationPreferenceService preferenceService;
    private final RealtimeNotificationService realtimeNotificationService;
    private final EmailService emailService;

    public OrderEventListener(NotificationPreferenceService preferenceService,
                              RealtimeNotificationService realtimeNotificationService,
                              EmailService emailService) {
        this.preferenceService = preferenceService;
        this.realtimeNotificationService = realtimeNotificationService;
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderLifecycleEvent(OrderLifecycleEvent event) {
        try {
            NotificationPreference preferences = preferenceService.getOrCreate(event.userId());

            if (preferences.isRealtimeOrderStatusEnabled()) {
                realtimeNotificationService.sendOrderEvent(event);
            }

            if (event.type() == OrderLifecycleEvent.Type.ORDER_PLACED
                    && preferences.isOrderInvoiceEmailEnabled()) {
                // EmailService is @Async: mail delivery runs outside the order transaction.
                emailService.sendOrderInvoice(event);
            }
        } catch (Exception exception) {
            // The order is already committed. Never turn a successful checkout/status change
            // into an HTTP failure just because an optional notification failed.
            log.error("Could not process notifications for order {}: {}",
                    event.orderId(), exception.getMessage(), exception);
        }
    }
}
