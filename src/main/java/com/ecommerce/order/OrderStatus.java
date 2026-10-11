package com.ecommerce.order;

public enum OrderStatus {

	PLACED,
	CONFIRMED,
	SHIPPED, 
	DELIVERED, 
	CANCELLED;
	
	public boolean canTransitionTo(OrderStatus next) {
        switch (this) {
            case PLACED:
                return next == CONFIRMED || next == CANCELLED;
            case CONFIRMED:
                return next == SHIPPED || next == CANCELLED;
            case SHIPPED:
                return next == DELIVERED;
            case DELIVERED:
                return false;
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }
}
