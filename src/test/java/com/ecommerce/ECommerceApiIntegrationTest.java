package com.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ecommerce.ai.dto.ShoppingPreferencesUpdateDTO;
import com.ecommerce.auth.Role;
import com.ecommerce.auth.User;
import com.ecommerce.auth.UserRepository;
import com.ecommerce.coupon.Coupon;
import com.ecommerce.dto.AuthRequest;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.CouponApplyRequest;
import com.ecommerce.dto.OrderStatusRequestUpdate;
import com.ecommerce.dto.PaymentInitiateRequest;
import com.ecommerce.dto.response.OrderResponseDTO;
import com.ecommerce.dto.response.PaymentResponseDTO;
import com.ecommerce.notification.dto.NotificationPreferenceUpdateRequest;
import com.ecommerce.order.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ECommerceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    private static String customerToken;
    private static String sellerToken;
    private static String adminToken;

    private static Long categoryId;
    private static Long productId;
    private static Long addressId;
    private static Long cartItemId;
    private static Long orderId;
    private static String paymentTxnId;
    private static Long reviewId;

    private static final String TIMESTAMP = String.valueOf(System.currentTimeMillis());
    private static final String CUSTOMER_EMAIL = "junit_cust_" + TIMESTAMP + "@test.com";
    private static final String SELLER_EMAIL = "junit_seller_" + TIMESTAMP + "@test.com";
    private static final String ADMIN_EMAIL = "junit_admin_" + TIMESTAMP + "@test.com";
    private static final String PASSWORD = "Password123!";

    // =========================================================================
    // 1. AUTHENTICATION MODULE TESTS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Auth: Register Customer, Seller, and Admin accounts")
    void testAuthRegistration() throws Exception {
        AuthRequest customerReq = new AuthRequest();
        customerReq.setName("JUnit Customer");
        customerReq.setEmail(CUSTOMER_EMAIL);
        customerReq.setPassword(PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerReq)))
                .andExpect(status().isOk());

        AuthRequest sellerReq = new AuthRequest();
        sellerReq.setName("JUnit Seller");
        sellerReq.setEmail(SELLER_EMAIL);
        sellerReq.setPassword(PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellerReq)))
                .andExpect(status().isOk());

        AuthRequest adminReq = new AuthRequest();
        adminReq.setName("JUnit Admin");
        adminReq.setEmail(ADMIN_EMAIL);
        adminReq.setPassword(PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isOk());

        // Update roles directly in DB
        User seller = userRepository.findByEmail(SELLER_EMAIL).orElseThrow();
        seller.setRole(Role.SELLER);
        userRepository.save(seller);

        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    @Test
    @Order(2)
    @DisplayName("Auth: Login all users and obtain JWT tokens")
    void testAuthLogin() throws Exception {
        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail(CUSTOMER_EMAIL);
        loginReq.setPassword(PASSWORD);

        MvcResult res = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();
        customerToken = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class).getToken();

        loginReq.setEmail(SELLER_EMAIL);
        res = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andReturn();
        sellerToken = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class).getToken();

        loginReq.setEmail(ADMIN_EMAIL);
        res = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        adminToken = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class).getToken();

        assertThat(customerToken).isNotBlank();
        assertThat(sellerToken).isNotBlank();
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("Auth: Get current profile (/api/auth/me) and refresh token")
    void testAuthMeAndRefresh() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // =========================================================================
    // 2. CATEGORY MODULE TESTS
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Category: Role-based authorization & CRUD operations")
    void testCategoryCrud() throws Exception {
        // Customer cannot create category (RBAC 403)
        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"UnauthorizedCat\",\"description\":\"test\"}"))
                .andExpect(status().isForbidden());

        // Admin creates category
        String catJson = "{\"name\":\"JUnit Electronics " + TIMESTAMP + "\",\"description\":\"Electronics and Gadgets\"}";
        MvcResult res = mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(catJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        categoryId = root.get("id").asLong();

        // Admin gets all categories (Redis cache test)
        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Admin updates category
        String updateCatJson = "{\"name\":\"JUnit Electronics Updated " + TIMESTAMP + "\",\"description\":\"Updated Description\"}";
        mockMvc.perform(put("/api/categories/" + categoryId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateCatJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    // =========================================================================
    // 3. PRODUCT MODULE TESTS
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Product: Create, Search, Get, Update, Bulk-Import")
    void testProductCrud() throws Exception {
        // Customer cannot create product
        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":10,\"stock\":5}"))
                .andExpect(status().isForbidden());

        // Seller creates product
        String prodJson = String.format("""
                {
                    "name": "JUnit Wireless Headphones %s",
                    "description": "High fidelity over-ear bluetooth headphones",
                    "price": 199.99,
                    "stock": 40,
                    "imageUrl": "https://example.com/headphones.jpg",
                    "category": { "id": %d }
                }
                """, TIMESTAMP, categoryId);

        MvcResult res = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(prodJson))
                .andExpect(status().isCreated())
                .andReturn();

        productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        assertThat(productId).isNotNull();

        // Public get product by id (cached in Redis)
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").isNotEmpty());

        // Public search products
        mockMvc.perform(get("/api/products")
                .param("keyword", "Headphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Seller updates product
        String updateProdJson = String.format("""
                {
                    "name": "JUnit Wireless Headphones Pro %s",
                    "description": "Updated high fidelity headphones",
                    "price": 249.99,
                    "stock": 35,
                    "category": { "id": %d }
                }
                """, TIMESTAMP, categoryId);

        mockMvc.perform(put("/api/products/" + productId)
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateProdJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(249.99));

        // Seller bulk imports products
        String bulkJson = String.format("""
                [
                    { "name": "Bulk Keyboard %s", "price": 89.99, "stock": 20, "category": { "id": %d } },
                    { "name": "Bulk Mouse %s", "price": 49.99, "stock": 30, "category": { "id": %d } }
                ]
                """, TIMESTAMP, categoryId, TIMESTAMP, categoryId);

        mockMvc.perform(post("/api/products/bulk-import")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkJson))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 4. ADDRESS MODULE TESTS
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Address: Create, Get, Update, Delete customer addresses")
    void testAddressCrud() throws Exception {
        String addrJson = """
                {
                    "line1": "456 JUnit Innovation Way",
                    "city": "Bengaluru",
                    "state": "Karnataka",
                    "zipCode": "560001",
                    "isDefault": true
                }
                """;

        MvcResult res = mockMvc.perform(post("/api/addresses")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addrJson))
                .andExpect(status().isCreated())
                .andReturn();

        addressId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/addresses")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        String updateAddrJson = """
                {
                    "line1": "456 JUnit Innovation Way, Suite 200",
                    "city": "Bengaluru",
                    "state": "Karnataka",
                    "zipCode": "560001",
                    "isDefault": true
                }
                """;

        mockMvc.perform(put("/api/addresses/" + addressId)
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateAddrJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line1").value("456 JUnit Innovation Way, Suite 200"));
    }

    // =========================================================================
    // 5. CART MODULE TESTS
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Cart: Add item, update quantity, remove item, clear cart")
    void testCartOperations() throws Exception {
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Add item
        CartItemRequest addReq = new CartItemRequest();
        addReq.setProductId(productId);
        addReq.setQuantity(2);

        MvcResult res = mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andReturn();

        cartItemId = objectMapper.readTree(res.getResponse().getContentAsString()).get("items").get(0).get("id").asLong();

        // Update quantity
        CartItemRequest updateReq = new CartItemRequest();
        updateReq.setProductId(productId);
        updateReq.setQuantity(3);

        mockMvc.perform(put("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // Remove item
        mockMvc.perform(delete("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        // Clear cart
        addReq.setQuantity(1);
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        // Add item again for subsequent checkout
        addReq.setQuantity(2);
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 6. COUPON MODULE TESTS
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Coupon: Create coupon (Admin), get all, apply to customer cart")
    void testCouponCrudAndApply() throws Exception {
        String couponCode = "JUNIT25_" + TIMESTAMP;
        Coupon coupon = new Coupon(couponCode, Coupon.DiscountType.PERCENTAGE, new BigDecimal("25.0"),
                LocalDate.now().plusDays(30), new BigDecimal("50.0"), List.of());

        mockMvc.perform(post("/api/coupons")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(couponCode));

        mockMvc.perform(get("/api/coupons")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        CouponApplyRequest applyReq = new CouponApplyRequest();
        applyReq.setCode(couponCode);

        mockMvc.perform(post("/api/coupons/apply")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 7. ORDER & PAYMENT MODULE TESTS (Full Lifecycle)
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Order & Payment: Checkout -> Initiate Payment -> Webhook -> Shipping -> Delivery")
    void testOrderAndPaymentLifecycle() throws Exception {
        // Checkout
        MvcResult checkoutRes = mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + customerToken)
                .param("shippingAddress", "456 JUnit Innovation Way, Bengaluru")
                .param("couponCode", "JUNIT25_" + TIMESTAMP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn();

        OrderResponseDTO orderDto = objectMapper.readValue(checkoutRes.getResponse().getContentAsString(), OrderResponseDTO.class);
        orderId = orderDto.orderId();
        assertThat(orderId).isNotNull();

        // Payment: Initiate
        PaymentInitiateRequest payReq = new PaymentInitiateRequest();
        payReq.setOrderId(orderId);
        payReq.setPaymentMethod("NET_BANKING");

        MvcResult payRes = mockMvc.perform(post("/api/payments/initiate")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        PaymentResponseDTO paymentDto = objectMapper.readValue(payRes.getResponse().getContentAsString(), PaymentResponseDTO.class);
        paymentTxnId = paymentDto.transactionId();

        // Payment: Get Status
        mockMvc.perform(get("/api/payments/" + orderId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId));

        // Payment: Webhook
        mockMvc.perform(post("/api/payments/webhook")
                .header("Authorization", "Bearer " + customerToken)
                .param("transactionId", paymentTxnId)
                .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Order: Get My Orders
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Order: Get Detail
        mockMvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId));

        // Admin updates status: CONFIRMED -> SHIPPED -> DELIVERED
        OrderStatusRequestUpdate statusReq = new OrderStatusRequestUpdate();
        statusReq.setStatus(OrderStatus.SHIPPED);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        statusReq.setStatus(OrderStatus.DELIVERED);
        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @Order(10)
    @DisplayName("Order: Test order cancellation")
    void testOrderCancellation() throws Exception {
        CartItemRequest addReq = new CartItemRequest();
        addReq.setProductId(productId);
        addReq.setQuantity(1);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        MvcResult res = mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + customerToken)
                .param("shippingAddress", "Temporary Address"))
                .andExpect(status().isCreated())
                .andReturn();

        Long cancelOrderId = objectMapper.readValue(res.getResponse().getContentAsString(), OrderResponseDTO.class).orderId();

        mockMvc.perform(post("/api/orders/" + cancelOrderId + "/cancel")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // =========================================================================
    // 8. WISHLIST MODULE TESTS
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("Wishlist: Add product, get wishlist, remove product")
    void testWishlistCrud() throws Exception {
        mockMvc.perform(post("/api/wishlist/" + productId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/wishlist")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());

        mockMvc.perform(delete("/api/wishlist/" + productId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 9. REVIEW MODULE TESTS
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Review: Add review for delivered product, view reviews, delete review")
    void testReviewLifecycle() throws Exception {
        String revJson = """
                {
                    "rating": 5,
                    "comment": "Outstanding battery and sound quality!"
                }
                """;

        MvcResult res = mockMvc.perform(post("/api/products/" + productId + "/reviews")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(revJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andReturn();

        reviewId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/products/" + productId + "/reviews/average-rating"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/products/" + productId + "/reviews/" + reviewId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 10. NOTIFICATION PREFERENCES MODULE TESTS
    // =========================================================================

    @Test
    @Order(13)
    @DisplayName("Notification: Get and update user notification preferences")
    void testNotificationPreferences() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtimeOrderStatusEnabled").isBoolean());

        NotificationPreferenceUpdateRequest updReq =
                new NotificationPreferenceUpdateRequest(true, false);

        mockMvc.perform(put("/api/notifications/preferences")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderInvoiceEmailEnabled").value(false));
    }

    // =========================================================================
    // 11. AI SHOPPING & PREFERENCE MODULE TESTS
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("AI Shopping: Manage shopping preferences & get personalized recommendations")
    void testAiShoppingAndPreferences() throws Exception {
        // Get Preferences
        mockMvc.perform(get("/api/ai/preferences")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Update Preferences
        ShoppingPreferencesUpdateDTO updateDto = new ShoppingPreferencesUpdateDTO(
                true,
                new BigDecimal("50.0"),
                new BigDecimal("500.0"),
                List.of("Electronics"),
                List.of("wireless", "headphones")
        );

        mockMvc.perform(put("/api/ai/preferences")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalizationEnabled").value(true));

        // Personalized Recommendations (catalog-grounded)
        mockMvc.perform(post("/api/ai/personalized-recommendations")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray());

        // Delete Preferences
        mockMvc.perform(delete("/api/ai/preferences")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 12. CLEANUP TESTS
    // =========================================================================

    @Test
    @Order(15)
    @DisplayName("Cleanup: Delete product and address")
    void testCleanup() throws Exception {
        if (addressId != null) {
            mockMvc.perform(delete("/api/addresses/" + addressId)
                    .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNoContent());
        }
    }
}
