package com.ecommerce.ai.recommendation;

import com.ecommerce.ai.AiShoppingService;
import com.ecommerce.ai.dto.PersonalizedRecommendationRequestDTO;
import com.ecommerce.ai.dto.ShoppingIntentDTO;
import com.ecommerce.ai.dto.ShoppingRecommendationResponseDTO;
import com.ecommerce.auth.AuthService;
import com.ecommerce.auth.User;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class PersonalizedRecommendationController {

    private final AuthService authService;
    private final AiShoppingService aiShoppingService;
    private final PersonalizedRecommendationService personalizedRecommendationService;

    public PersonalizedRecommendationController(AuthService authService,
                                                AiShoppingService aiShoppingService,
                                                PersonalizedRecommendationService personalizedRecommendationService) {
        this.authService = authService;
        this.aiShoppingService = aiShoppingService;
        this.personalizedRecommendationService = personalizedRecommendationService;
    }

    @PostMapping("/personalized-recommendations")
    public ShoppingRecommendationResponseDTO recommend(
            @Valid @RequestBody(required = false) PersonalizedRecommendationRequestDTO request,
            Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        String message = request == null ? null : request.message();
        ShoppingIntentDTO intent = StringUtils.hasText(message)
                ? aiShoppingService.interpretNeeds(message)
                : new ShoppingIntentDTO("", "", "", null, null, List.of(), List.of(), List.of());
        return personalizedRecommendationService.recommend(user, intent);
    }
}
