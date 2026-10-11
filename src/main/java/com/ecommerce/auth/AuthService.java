package com.ecommerce.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.cart.Cart;
import com.ecommerce.cart.CartRepository;
import com.ecommerce.dto.AuthRequest;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.wishlist.Wishlist;
import com.ecommerce.wishlist.WishlistRepository;

@Service
public class AuthService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JwtService jwtService;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private CartRepository cartRepository;
	@Autowired
	private WishlistRepository wishlistRepository;

    @Transactional
    public String register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);

        // Create associated Cart and Wishlist
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlistRepository.save(wishlist);

        return "User registered successfully";
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setUserId(user.getId());
        return response;
    }

    public AuthResponse refresh(String oldToken) {
        String username = jwtService.extractUsername(oldToken);
        UserDetails userDetails = userRepository.findByEmail(username).orElseThrow();
        String newToken = jwtService.generateToken(userDetails);
        AuthResponse response = new AuthResponse();
        response.setToken(newToken);
        response.setEmail(username);
        response.setRole(userDetails.getAuthorities().iterator().next().getAuthority());
        response.setUserId(((User) userDetails).getId());
        return response;
    }

    public User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow();
    }
}
