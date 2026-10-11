package com.ecommerce.ai;

import com.ecommerce.ai.dto.ShoppingRecommendationRequestDTO;
import com.ecommerce.ai.dto.ShoppingRecommendationResponseDTO;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiShoppingController {

    private final AiShoppingService aiShoppingService;

    public AiShoppingController(AiShoppingService aiShoppingService) {
        this.aiShoppingService = aiShoppingService;
    }

    /**
     * Interprets a shopper's needs, searches the real product catalog, and returns
     * ranked product DTOs with catalog-grounded explanations.
     */
    @PostMapping("/recommendations")
    public ResponseEntity<ShoppingRecommendationResponseDTO> recommend(
            @Valid @RequestBody ShoppingRecommendationRequestDTO request) {
        return ResponseEntity.ok(aiShoppingService.recommend(request.message()));
    }
}
