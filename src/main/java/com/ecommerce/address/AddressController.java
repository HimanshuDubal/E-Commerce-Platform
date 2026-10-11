package com.ecommerce.address;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import com.ecommerce.dto.response.AddressResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

	@Autowired
	private AddressService addressService;
	@Autowired
	private AuthService authService;
	
	@GetMapping
    public ResponseEntity<List<AddressResponseDTO>> getMyAddresses(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        List<AddressResponseDTO> body = addressService.getAddressesForUser(user).stream()
                .map(AddressResponseDTO::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<AddressResponseDTO> createAddress(@Valid @RequestBody Address address, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        Address created = addressService.createAddress(user, address);
        return ResponseEntity.status(HttpStatus.CREATED).body(AddressResponseDTO.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> updateAddress(@PathVariable Long id,
                                                            @Valid @RequestBody Address address,
                                                            Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        Address updated = addressService.updateAddress(user, id, address);
        return ResponseEntity.ok(AddressResponseDTO.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        addressService.deleteAddress(user, id);
        return ResponseEntity.noContent().build();
    }

}
