package com.ecommerce.payment;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.common.exceptions.PaymentProcessingException;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.notification.event.OrderLifecycleEventPublisher;
import com.ecommerce.order.Order;
import com.ecommerce.order.OrderItem;
import com.ecommerce.order.OrderRepository;
import com.ecommerce.order.OrderStatus;
import com.ecommerce.product.Product;
import com.ecommerce.product.ProductRepository;

@Service
public class PaymentService {

    private final ProductRepository productRepository;

	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired 
	private OrderLifecycleEventPublisher orderEventPublisher;

    PaymentService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Payment initiatePayment(Long orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new PaymentProcessingException("Order is not in PLACED state");
        }
        if (order.getPayment() != null) {
            throw new PaymentProcessingException("Payment already initiated for this order");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId("txn_" + UUID.randomUUID());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment processWebhook(String transactionId, String status) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for transaction: " + transactionId));

        if (payment.getStatus() != PaymentStatus.PENDING) return payment;

        if ("SUCCESS".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.PLACED) {
                OrderStatus previousStatus = order.getStatus();
                order.setStatus(OrderStatus.CONFIRMED);
                Order savedOrder = orderRepository.save(order);
                orderEventPublisher.statusChanged(savedOrder, previousStatus);
            }
        } else if ("FAILED".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.FAILED);
            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.PLACED) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                }
                OrderStatus previousStatus = order.getStatus();
                order.setStatus(OrderStatus.CANCELLED);
                Order savedOrder = orderRepository.save(order);
                orderEventPublisher.statusChanged(savedOrder, previousStatus);
            }
        } else {
            throw new PaymentProcessingException("Unknown payment status: " + status);
        }

        return paymentRepository.save(payment);
    }

    public Payment getPaymentByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
    }

}
