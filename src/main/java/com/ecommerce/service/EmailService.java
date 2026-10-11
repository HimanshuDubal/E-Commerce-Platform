package com.ecommerce.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.ecommerce.notification.event.OrderLifecycleEvent;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendOrderInvoice(OrderLifecycleEvent event) {
        if (event.customerEmail() == null || event.customerEmail().isBlank()) {
            logger.warn("Skipping invoice email for order {} because the customer email is blank", event.orderId());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(event.customerEmail());
            if (fromAddress != null && !fromAddress.isBlank()) helper.setFrom(fromAddress);
            helper.setSubject("Your E-Commerce order bill #" + event.orderId());
            helper.setText(buildInvoiceHtml(event), true);
            mailSender.send(message);
            logger.info("Order invoice email sent for order {}", event.orderId());
        } catch (Exception exception) {
            
            logger.error("Unable to send invoice for order {}: {}", event.orderId(), exception.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmation(String toEmail, String orderId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            if (fromAddress != null && !fromAddress.isBlank()) message.setFrom(fromAddress);
            message.setSubject("Order Confirmation #" + orderId);
            message.setText("Thank you for your order. Your order #" + orderId + " has been placed.");
            mailSender.send(message);
            logger.info("Confirmation email sent for order {}", orderId);
        } catch (Exception exception) {
            logger.error("Unable to send order confirmation for order {}: {}", orderId, exception.getMessage());
        }
    }

    private String buildInvoiceHtml(OrderLifecycleEvent event) {
        StringBuilder rows = new StringBuilder();
        for (OrderLifecycleEvent.InvoiceLine line : event.items()) {
            rows.append("<tr>")
                .append("<td style='padding:10px;border-bottom:1px solid #eee;'>").append(escape(line.productName())).append("</td>")
                .append("<td style='padding:10px;text-align:center;border-bottom:1px solid #eee;'>")
                    .append(line.quantity() == null ? 0 : line.quantity()).append("</td>")
                .append("<td style='padding:10px;text-align:right;border-bottom:1px solid #eee;'>").append(money(line.unitPrice())).append("</td>")
                .append("<td style='padding:10px;text-align:right;border-bottom:1px solid #eee;'>").append(money(line.lineTotal())).append("</td>")
                .append("</tr>");
        }
        if (rows.length() == 0) {
            rows.append("<tr><td colspan='4' style='padding:10px;'>No item details were available.</td></tr>");
        }
        String customerName = event.customerName() == null || event.customerName().isBlank()
                ? "Customer" : escape(event.customerName());
        String date = event.occurredAt() == null ? "" : event.occurredAt().format(DATE_FORMAT);
        BigDecimal subtotal = event.items().stream()
                .map(line -> line.lineTotal() == null ? BigDecimal.ZERO : line.lineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = event.totalAmount() == null ? BigDecimal.ZERO : event.totalAmount();
        BigDecimal discount = subtotal.subtract(total).max(BigDecimal.ZERO);

        return """
                <!doctype html>
                <html><body style="margin:0;background:#f5f7fb;font-family:Arial,sans-serif;color:#1f2937;">
                  <div style="max-width:700px;margin:24px auto;background:#fff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;">
                    <div style="background:#172554;color:#fff;padding:24px;">
                      <h1 style="margin:0;font-size:24px;">Order bill</h1><p style="margin:8px 0 0;">Order #%s</p>
                    </div>
                    <div style="padding:24px;">
                      <p>Hello %s,</p><p>Thank you for shopping with us. Your order has been placed successfully.</p>
                      <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                        <tr><td style="padding:6px 0;color:#6b7280;">Order date</td><td style="padding:6px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:6px 0;color:#6b7280;">Status</td><td style="padding:6px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:6px 0;color:#6b7280;">Shipping address</td><td style="padding:6px 0;text-align:right;">%s</td></tr>
                      </table>
                      <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                        <thead><tr style="background:#f3f4f6;text-align:left;">
                          <th style="padding:10px;">Item</th><th style="padding:10px;text-align:center;">Qty</th>
                          <th style="padding:10px;text-align:right;">Unit price</th><th style="padding:10px;text-align:right;">Line total</th>
                        </tr></thead><tbody>%s</tbody>
                      </table>
                      <table style="width:100%%;border-collapse:collapse;margin-top:18px;">
                        <tr><td style="padding:5px 0;text-align:right;color:#6b7280;">Subtotal</td><td style="padding:5px 0;text-align:right;width:140px;">%s</td></tr>
                        <tr><td style="padding:5px 0;text-align:right;color:#6b7280;">Discount</td><td style="padding:5px 0;text-align:right;">-%s</td></tr>
                      </table>
                      <div style="margin-top:12px;padding-top:16px;border-top:2px solid #e5e7eb;text-align:right;">
                        <span style="color:#6b7280;">Order total</span>
                        <strong style="display:block;font-size:24px;margin-top:6px;">%s</strong>
                      </div>
                      <p style="margin-top:28px;color:#6b7280;font-size:12px;">This email is your order bill. Keep it for your records.</p>
                    </div>
                  </div>
                </body></html>
                """.formatted(event.orderId(), customerName, escape(date), escape(event.status()),
                escape(event.shippingAddress()), rows, money(subtotal), money(discount), money(total));
    }

    private String escape(String value) {
        return value == null ? "—" : HtmlUtils.htmlEscape(value);
    }

    private String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return "INR " + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
