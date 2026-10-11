package com.ecommerce.dto;

import com.ecommerce.order.OrderStatus;

public class OrderStatusRequestUpdate {

	private OrderStatus status;
	
	public OrderStatusRequestUpdate() {
		// TODO Auto-generated constructor stub
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	
}
