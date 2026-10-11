package com.ecommerce.ai.preference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserShoppingPreferenceRepository extends JpaRepository<UserShoppingPreference, Long> {
    Optional<UserShoppingPreference> findByUserId(Long userId);
}
