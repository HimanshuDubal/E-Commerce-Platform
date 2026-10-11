package com.ecommerce.dto.response;

import com.ecommerce.auth.Role;
import com.ecommerce.auth.User;

import java.time.LocalDateTime;

public record UserResponseDTO(
		Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponseDTO from(User user) {
        if (user == null) return null;
        return new UserResponseDTO(
        		user.getId(),
                user.getName(), user.getEmail(), user.getRole(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
