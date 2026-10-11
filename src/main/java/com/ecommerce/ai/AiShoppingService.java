package com.ecommerce.ai;

import com.ecommerce.ai.dto.ShoppingIntentDTO;
import com.ecommerce.ai.dto.ShoppingRecommendationResponseDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/** Coordinates intent extraction and catalog search without holding a DB transaction during the AI call. */
@Service
public class AiShoppingService {

    private static final String INTENT_EXTRACTION_INSTRUCTIONS = """
            You are ShopMate, a shopping-requirement parser for an e-commerce catalog.
            Convert the customer's request into the requested structured output only.
            Do not recommend or invent products. Extract only information the customer stated
            or clearly implied. Use null for unknown budgets, empty strings for unknown text,
            and empty arrays for unknown lists. Budget values must be numeric amounts in the
            user's stated currency; do not convert currencies. Keep productType short (for example,
            'laptop', 'phone', 'headphones'). Use category only if the likely catalog category is clear.
            Put explicit hard requirements in requiredFeatures and softer wishes in preferences.
            searchKeywords should contain a few short terms likely to occur in product names or
            descriptions. If the customer did not state a budget, do not invent one.
            """;

    private final ChatClient chatClient;
    private final ProductRecommendationCatalogService catalogService;

    public AiShoppingService(ChatClient shopMateChatClient,
                             ProductRecommendationCatalogService catalogService) {
        this.chatClient = shopMateChatClient;
        this.catalogService = catalogService;
    }

    public ShoppingRecommendationResponseDTO recommend(String shopperMessage) {
    	ShoppingIntentDTO intent = interpretNeeds(shopperMessage);
        List<com.ecommerce.ai.dto.ProductRecommendationDTO> recommendations = catalogService.findRecommendations(intent);

        String message;
        if (recommendations.isEmpty()) {
            message = "I couldn't find in-stock products matching those search terms and budget in the current catalog. " +
                    "Try broader keywords, remove one required feature, or adjust your budget.";
        } else {
            message = "I found " + recommendations.size() + " in-stock option(s) based on your request. " +
                    "Prices and availability come from the current catalog. Check each product's unverified requirements " +
                    "before purchasing, because the catalog may not contain every technical specification.";
        }
        return new ShoppingRecommendationResponseDTO(message, intent, recommendations);
    }

    private ShoppingIntentDTO extractIntent(String shopperMessage) {
        try {
            ShoppingIntentDTO intent = chatClient.prompt()
                    .system(INTENT_EXTRACTION_INSTRUCTIONS)
                    .user("Extract shopping requirements from this message:\n" + shopperMessage)
                    .call()
                    .entity(ShoppingIntentDTO.class);
            if (intent == null) {
                throw new IllegalStateException("The AI returned no shopping intent");
            }
            return normalizeIntent(intent);
        } catch (Exception ex) {
            // Do not echo provider payloads, prompts, or credentials back to the client.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "ShopMate AI is temporarily unavailable. Check the OpenAI API key, API billing, and service logs.", ex);
        }
    }

    private ShoppingIntentDTO normalizeIntent(ShoppingIntentDTO intent) {
        BigDecimal min = nonNegativeOrNull(intent.minBudget());
        BigDecimal max = nonNegativeOrNull(intent.maxBudget());
        if (min != null && max != null && min.compareTo(max) > 0) {
            // Inconsistent extracted bounds are safer as an unbounded minimum than as a reversed range.
            min = null;
        }
        return new ShoppingIntentDTO(clean(intent.productType()), clean(intent.category()), clean(intent.useCase()),
                min, max, cleanList(intent.requiredFeatures()), cleanList(intent.preferences()),
                cleanList(intent.searchKeywords()));
    }

    private void validateIntent(ShoppingIntentDTO intent) {
        if (isBlank(intent.productType()) && isBlank(intent.category()) &&
                isBlank(intent.useCase()) && safe(intent.searchKeywords()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please specify the type of product you are looking for.");
        }
    }

    private static BigDecimal nonNegativeOrNull(BigDecimal value) {
        return value != null && value.signum() >= 0 ? value : null;
    }

    private static List<String> cleanList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(AiShoppingService::clean)
                .filter(s -> !isBlank(s)).distinct().limit(12).toList();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }
    
    public ShoppingIntentDTO interpretNeeds(String shopperMessage) {
        ShoppingIntentDTO intent = extractIntent(shopperMessage);
        validateIntent(intent);
        return intent;
    }
}
