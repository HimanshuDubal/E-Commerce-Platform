package com.ecommerce.notification;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import com.ecommerce.notification.dto.NotificationPreferenceResponseDTO;
import com.ecommerce.notification.dto.NotificationPreferenceUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final AuthService authService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService,
                                            AuthService authService) {
        this.preferenceService = preferenceService;
        this.authService = authService;
    }

    @GetMapping("/preferences")
    public NotificationPreferenceResponseDTO getPreferences(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return NotificationPreferenceResponseDTO.from(preferenceService.getOrCreate(user.getId()));
    }

    @PutMapping("/preferences")
    public NotificationPreferenceResponseDTO updatePreferences(
            @Valid @RequestBody NotificationPreferenceUpdateRequest request,
            Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return NotificationPreferenceResponseDTO.from(preferenceService.update(
                user.getId(),
                request.realtimeOrderStatusEnabled(),
                request.orderInvoiceEmailEnabled()));
    }
}
