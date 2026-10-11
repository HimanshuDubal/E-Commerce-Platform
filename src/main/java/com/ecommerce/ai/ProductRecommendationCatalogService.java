package com.ecommerce.ai;

import com.ecommerce.ai.dto.ProductRecommendationDTO;
import com.ecommerce.ai.dto.ShoppingIntentDTO;
import com.ecommerce.category.Category;
import com.ecommerce.product.Product;
import com.ecommerce.product.ProductRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductRecommendationCatalogService {

    private static final int CANDIDATE_LIMIT = 100;
    private static final int RECOMMENDATION_LIMIT = 5;
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "under", "less",
            "than", "need", "want", "looking", "buy", "good", "best", "please", "some", "that",
            "this", "from", "into", "have", "has", "are", "was", "its", "use", "using", "my");

    private final ProductRepository productRepository;

    public ProductRecommendationCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductRecommendationDTO> findRecommendations(ShoppingIntentDTO intent) {
        List<Product> candidates = productRepository.findAll(buildSpecification(intent),
                PageRequest.of(0, CANDIDATE_LIMIT, Sort.by(Sort.Direction.ASC, "price"))).getContent();

        return candidates.stream()
                .map(product -> toRecommendation(product, intent))
                .sorted(Comparator.comparingInt(ProductRecommendationDTO::matchScore).reversed()
                        .thenComparing(ProductRecommendationDTO::price,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(RECOMMENDATION_LIMIT)
                .toList();
    }

    private Specification<Product> buildSpecification(ShoppingIntentDTO intent) {
        List<String> sourceTerms = new ArrayList<>();
        sourceTerms.add(intent.productType());
        sourceTerms.add(intent.category());
        sourceTerms.add(intent.useCase());
        sourceTerms.addAll(safe(intent.searchKeywords()));
        sourceTerms.addAll(safe(intent.requiredFeatures()));
        sourceTerms.addAll(safe(intent.preferences()));
        List<String> terms = sourceTerms.stream()
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> words(s).stream())
                .filter(term -> term.length() >= 3 && !STOP_WORDS.contains(term))
                .distinct().limit(15).toList();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThan(root.get("stock"), 0));
            if (intent.minBudget() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), intent.minBudget()));
            }
            if (intent.maxBudget() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), intent.maxBudget()));
            }

            if (!terms.isEmpty()) {
                Join<Product, Category> category = root.join("category", JoinType.LEFT);
                List<Predicate> matches = new ArrayList<>();
                for (String term : terms) {
                    String pattern = "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
                    matches.add(cb.like(cb.lower(root.get("name")), pattern, '\\'));
                    matches.add(cb.like(cb.lower(root.get("description")), pattern, '\\'));
                    matches.add(cb.like(cb.lower(category.get("name")), pattern, '\\'));
                }
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ProductRecommendationDTO toRecommendation(Product product, ShoppingIntentDTO intent) {
        String categoryName = product.getCategory() == null ? null : product.getCategory().getName();
        String searchableText = normalizeForMatch(String.join(" ", safe(product.getName()),
                safe(product.getDescription()), safe(categoryName)));

        List<String> requirements = safe(intent.requiredFeatures());
        List<String> matched = requirements.stream()
                .filter(feature -> containsNormalized(searchableText, feature)).toList();
        List<String> unverified = requirements.stream()
                .filter(feature -> !containsNormalized(searchableText, feature)).toList();

        int score = 0;
        score += phraseScore(searchableText, intent.productType(), 8);
        score += phraseScore(searchableText, intent.category(), 5);
        for (String keyword : safe(intent.searchKeywords())) score += phraseScore(searchableText, keyword, 3);
        for (String feature : requirements) score += containsNormalized(searchableText, feature) ? 6 : 0;
        for (String preference : safe(intent.preferences())) score += containsNormalized(searchableText, preference) ? 2 : 0;
        if (intent.maxBudget() != null && product.getPrice() != null) {
            BigDecimal headroom = intent.maxBudget().subtract(product.getPrice());
            if (headroom.signum() >= 0) score += headroom.compareTo(intent.maxBudget().multiply(new BigDecimal("0.15"))) <= 0 ? 2 : 1;
        }

        List<String> reasons = new ArrayList<>();
        if (intent.productType() != null && !intent.productType().isBlank()) {
            reasons.add("catalog text matches your product request ('" + intent.productType() + "')");
        }
        if (intent.maxBudget() != null) reasons.add("within your maximum budget");
        if (product.getStock() != null && product.getStock() > 0) reasons.add("currently in stock");
        if (!matched.isEmpty()) reasons.add("catalog text mentions: " + String.join(", ", matched));
        String reason = reasons.isEmpty()
                ? "This item matched the catalog search; review the specifications before purchasing."
                : "Recommended because it is " + String.join("; ", reasons) + ".";
        if (!unverified.isEmpty()) {
            reason += " The catalog does not verify these requested features: " + String.join(", ", unverified) + ".";
        }

        return new ProductRecommendationDTO(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), product.getImageUrl(), categoryName, score,
                matched, unverified, reason);
    }

    private static int phraseScore(String searchableText, String phrase, int points) {
        if (phrase == null || phrase.isBlank()) return 0;
        String normalizedPhrase = normalizeForMatch(phrase);
        if (normalizedPhrase.isEmpty()) return 0;
        if (searchableText.contains(normalizedPhrase)) return points;
        List<String> tokens = words(normalizedPhrase);
        long hits = tokens.stream().filter(token -> searchableText.contains(token)).count();
        if (hits == 0) return 0;
        return (int) Math.max(1, Math.round(points * (hits / (double) tokens.size())));
    }

    private static List<String> words(String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(normalizeForMatch(value).split("\\s+"));
    }

    private static String normalizeForMatch(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static boolean containsNormalized(String haystack, String needle) {
        String normalizedNeedle = normalizeForMatch(needle);
        if (normalizedNeedle.isEmpty()) return false;
        return (" " + haystack + " ").contains(" " + normalizedNeedle + " ") || haystack.contains(normalizedNeedle);
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }
    
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
    
}
