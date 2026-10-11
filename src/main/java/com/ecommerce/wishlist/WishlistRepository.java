package com.ecommerce.wishlist;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long>{

	Optional<Wishlist> findByUserId(Long userId);
}
