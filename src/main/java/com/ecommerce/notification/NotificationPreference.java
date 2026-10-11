package com.ecommerce.notification;

import com.ecommerce.auth.User;
import com.ecommerce.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean realtimeOrderStatusEnabled = true;

    @Column(nullable = false)
    private boolean orderInvoiceEmailEnabled = true;

    public NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isRealtimeOrderStatusEnabled() {
        return realtimeOrderStatusEnabled;
    }

    public void setRealtimeOrderStatusEnabled(boolean realtimeOrderStatusEnabled) {
        this.realtimeOrderStatusEnabled = realtimeOrderStatusEnabled;
    }

    public boolean isOrderInvoiceEmailEnabled() {
        return orderInvoiceEmailEnabled;
    }

    public void setOrderInvoiceEmailEnabled(boolean orderInvoiceEmailEnabled) {
        this.orderInvoiceEmailEnabled = orderInvoiceEmailEnabled;
    }
}
