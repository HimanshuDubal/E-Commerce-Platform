package com.ecommerce.product;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import com.ecommerce.dto.response.ProductResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	@Autowired
	private ProductService productService;
	@Autowired
	private AuthService authService;
	
	@GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> searchProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
		Page<ProductResponseDTO> body = productService
                .searchProducts(categoryId, minPrice, maxPrice, keyword, pageable)
                .map(ProductResponseDTO::from);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ProductResponseDTO.from(productService.getProductById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody Product product, Authentication authentication) {
    	User seller = authService.getCurrentUser(authentication);
        Product created = productService.createProduct(product, seller);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ProductResponseDTO.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product,
                                 Authentication authentication) {
        User currentUser = authService.getCurrentUser(authentication);
        Product updated = productService.updateProduct(id, product, currentUser);
        return ResponseEntity.ok(ProductResponseDTO.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, Authentication authentication) {
        User currentUser = authService.getCurrentUser(authentication);
        productService.deleteProduct(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-import")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<String> bulkImport(@RequestBody List<Product> products, Authentication authentication) {
        User seller = authService.getCurrentUser(authentication);
        productService.bulkImport(products, seller);
        return ResponseEntity.ok("Products imported successfully");
    }

}
