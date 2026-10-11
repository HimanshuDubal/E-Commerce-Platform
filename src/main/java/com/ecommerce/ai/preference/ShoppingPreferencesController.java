package com.ecommerce.ai.preference;

import com.ecommerce.ai.dto.ShoppingPreferencesResponseDTO;
import com.ecommerce.ai.dto.ShoppingPreferencesUpdateDTO;
import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/preferences")
public class ShoppingPreferencesController {

    private final ShoppingPreferencesService preferencesService;
    private final AuthService authService;

    public ShoppingPreferencesController(ShoppingPreferencesService preferencesService, AuthService authService) {
        this.preferencesService = preferencesService;
        this.authService = authService;
    }

    @GetMapping
    public ShoppingPreferencesResponseDTO getPreferences(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return preferencesService.getPreferences(user);
    }

    @PutMapping
    public ShoppingPreferencesResponseDTO updatePreferences(
            @Valid @RequestBody ShoppingPreferencesUpdateDTO request,
            Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return preferencesService.updatePreferences(user, request);
    }

    /** Deletes the saved preference profile. It does not delete orders or wishlist items. */
    @DeleteMapping
    public ResponseEntity<Void> deletePreferences(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        preferencesService.deletePreferences(user);
        return ResponseEntity.noContent().build();
    }
}
