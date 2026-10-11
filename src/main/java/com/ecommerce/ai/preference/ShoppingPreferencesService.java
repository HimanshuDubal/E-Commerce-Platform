package com.ecommerce.ai.preference;

import com.ecommerce.ai.dto.ShoppingPreferencesResponseDTO;
import com.ecommerce.ai.dto.ShoppingPreferencesUpdateDTO;
import com.ecommerce.ai.preference.UserShoppingPreference;
import com.ecommerce.ai.preference.UserShoppingPreferenceRepository;
import com.ecommerce.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ShoppingPreferencesService {

    private final UserShoppingPreferenceRepository repository;

    public ShoppingPreferencesService(UserShoppingPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ShoppingPreferencesResponseDTO getPreferences(User user) {
        return repository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElseGet(() -> new ShoppingPreferencesResponseDTO(false, null, null, List.of(), List.of()));
    }

    @Transactional
    public ShoppingPreferencesResponseDTO updatePreferences(User user, ShoppingPreferencesUpdateDTO request) {
        validateBudgetRange(request.minBudget(), request.maxBudget());
        UserShoppingPreference preference = repository.findByUserId(user.getId()).orElseGet(() -> {
            UserShoppingPreference created = new UserShoppingPreference();
            created.setUser(user);
            return created;
        });

        preference.setPersonalizationEnabled(Boolean.TRUE.equals(request.personalizationEnabled()));
        preference.setMinBudget(request.minBudget());
        preference.setMaxBudget(request.maxBudget());
        preference.setPreferredCategories(new LinkedHashSet<>(normalizeList(request.preferredCategories())));
        preference.setPreferredKeywords(new LinkedHashSet<>(normalizeList(request.preferredKeywords())));

        return toResponse(repository.save(preference));
    }

    @Transactional
    public void deletePreferences(User user) {
        repository.findByUserId(user.getId()).ifPresent(repository::delete);
    }

    private void validateBudgetRange(BigDecimal minBudget, BigDecimal maxBudget) {
        if (minBudget != null && maxBudget != null && minBudget.compareTo(maxBudget) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "minBudget cannot be greater than maxBudget");
        }
    }

    private ShoppingPreferencesResponseDTO toResponse(UserShoppingPreference preference) {
        return new ShoppingPreferencesResponseDTO(
                preference.isPersonalizationEnabled(),
                preference.getMinBudget(),
                preference.getMaxBudget(),
                sorted(preference.getPreferredCategories()),
                sorted(preference.getPreferredKeywords())
        );
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : values) {
            if (value == null) continue;
            String cleaned = value.trim().replaceAll("\\s+", " ");
            if (cleaned.isEmpty()) continue;
            unique.putIfAbsent(cleaned.toLowerCase(Locale.ROOT), cleaned);
        }
        return new ArrayList<>(unique.values());
    }

    private static List<String> sorted(Set<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
