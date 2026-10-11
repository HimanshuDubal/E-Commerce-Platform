package com.ecommerce.payment;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ecommerce.dto.PaymentInitiateRequest;
import com.ecommerce.dto.response.PaymentResponseDTO;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;
	
	@PostMapping("/initiate")
    public ResponseEntity<PaymentResponseDTO> initiatePayment(@RequestBody PaymentInitiateRequest request) {
		Payment payment = paymentService.initiatePayment(request.getOrderId(), request.getPaymentMethod());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/payments/{orderId}").buildAndExpand(request.getOrderId()).toUri();
        return ResponseEntity.created(location).body(PaymentResponseDTO.from(payment));
    }

    @PostMapping("/webhook")
    public ResponseEntity<PaymentResponseDTO> webhook(@RequestParam String transactionId,
                                     @RequestParam String status) {
        Payment payment = paymentService.processWebhook(transactionId, status);
        return ResponseEntity.ok(PaymentResponseDTO.from(payment));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(@PathVariable Long orderId) {
        return ResponseEntity.ok(PaymentResponseDTO.from(paymentService.getPaymentByOrder(orderId)));
    }

}
