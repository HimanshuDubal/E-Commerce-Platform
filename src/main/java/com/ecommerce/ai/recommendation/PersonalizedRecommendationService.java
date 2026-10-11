package com.ecommerce.ai.recommendation;

import com.ecommerce.ai.dto.ProductRecommendationDTO;
import com.ecommerce.ai.dto.ShoppingIntentDTO;
import com.ecommerce.ai.dto.ShoppingRecommendationResponseDTO;
import com.ecommerce.ai.preference.UserShoppingPreference;
import com.ecommerce.ai.preference.UserShoppingPreferenceRepository;
import com.ecommerce.auth.User;
import com.ecommerce.order.Order;
import com.ecommerce.order.OrderItem;
import com.ecommerce.order.OrderRepository;
import com.ecommerce.order.OrderStatus;
import com.ecommerce.product.Product;
import com.ecommerce.product.ProductRepository;
import com.ecommerce.category.Category;
import com.ecommerce.wishlist.WishlistRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PersonalizedRecommendationService {

    private static final int CANDIDATE_LIMIT = 250;
    private static final int RECOMMENDATION_LIMIT = 5;
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "under", "less", "than",
            "need", "want", "looking", "buy", "good", "best", "please", "some", "that", "this", "from",
            "into", "have", "has", "are", "was", "its", "use", "using", "my", "your", "product", "products");

    private final ProductRepository productRepository;
    private final UserShoppingPreferenceRepository preferenceRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderRepository orderRepository;

    public PersonalizedRecommendationService(ProductRepository productRepository,
                                             UserShoppingPreferenceRepository preferenceRepository,
                                             WishlistRepository wishlistRepository,
                                             OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.preferenceRepository = preferenceRepository;
        this.wishlistRepository = wishlistRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public ShoppingRecommendationResponseDTO recommend(User user, ShoppingIntentDTO intent) {
        UserShoppingPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Enable personalization and save your preferences before requesting personalized recommendations."));
        if (!preference.isPersonalizationEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Personalized recommendations are disabled. Enable personalization in your preferences first.");
        }

        PreferenceSignals signals = collectSignals(user);

        BigDecimal calculatedMinBudget =
                effectiveMinBudget(intent, preference);

        BigDecimal calculatedMaxBudget =
                effectiveMaxBudget(intent, preference);

        if (calculatedMinBudget != null
                && calculatedMaxBudget != null
                && calculatedMinBudget.compareTo(calculatedMaxBudget) > 0) {

            // A one-sided budget from the current message takes precedence
            // over a conflicting saved preference.

            if (intent.minBudget() != null && intent.maxBudget() == null) {
                calculatedMaxBudget = null;
            } else if (intent.maxBudget() != null && intent.minBudget() == null) {
                calculatedMinBudget = null;
            }
        }

        final BigDecimal minBudget = calculatedMinBudget;
        final BigDecimal maxBudget = calculatedMaxBudget;

        List<String> searchTerms = intentTerms(intent);
        if (searchTerms.isEmpty()) {
            searchTerms = new ArrayList<>();
            searchTerms.addAll(safe(preference.getPreferredCategories()));
            searchTerms.addAll(safe(preference.getPreferredKeywords()));
            searchTerms.addAll(signals.wishlistCategories());
            searchTerms.addAll(signals.purchasedCategories());
            searchTerms.addAll(signals.interestTerms());
            searchTerms = normalizeTerms(searchTerms);
        }

        List<Product> candidates = productRepository.findAll(
                buildSpecification(searchTerms, minBudget, maxBudget),
                PageRequest.of(0, CANDIDATE_LIMIT, Sort.by(Sort.Direction.ASC, "price"))).getContent();

        List<ProductRecommendationDTO> recommendations = candidates.stream()
                .map(product -> toRecommendation(product, intent, preference, signals, minBudget, maxBudget))
                .sorted(Comparator.comparingInt(ProductRecommendationDTO::matchScore).reversed()
                        .thenComparing(ProductRecommendationDTO::price,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(RECOMMENDATION_LIMIT)
                .toList();

        String message;
        if (recommendations.isEmpty()) {
            message = "I couldn't find in-stock products matching your request and saved budget in the current catalog. "
                    + "Try different keywords or update your saved preferences.";
        } else {
            List<String> sources = new ArrayList<>();
            if (!safe(preference.getPreferredCategories()).isEmpty() || !safe(preference.getPreferredKeywords()).isEmpty()) {
                sources.add("saved preferences");
            }
            if (!signals.wishlistCategories().isEmpty()) sources.add("wishlist categories");
            if (!signals.purchasedCategories().isEmpty()) sources.add("non-cancelled order history");
            if (!intentTerms(intent).isEmpty()) sources.add("your current request");
            if (sources.isEmpty()) {
                message = "Here are available products within your saved budget. Add preferred categories or keywords, or use your wishlist and order history, to make recommendations more personalized.";
            } else {
                message = "I ranked " + recommendations.size() + " in-stock product(s) using "
                        + String.join(", ", sources) + ". Prices and stock come from the current catalog.";
            }
        }
        return new ShoppingRecommendationResponseDTO(message, intent, recommendations);
    }

    private PreferenceSignals collectSignals(User user) {
        Set<Long> wishlistProductIds = new LinkedHashSet<>();
        Set<String> wishlistCategories = new LinkedHashSet<>();
        Set<String> purchasedCategories = new LinkedHashSet<>();
        Set<String> interestTerms = new LinkedHashSet<>();

        wishlistRepository.findByUserId(user.getId()).ifPresent(wishlist -> {
            for (Product product : wishlist.getProducts()) {
                if (product == null) continue;
                if (product.getId() != null) wishlistProductIds.add(product.getId());
                addProductSignals(product, wishlistCategories, interestTerms);
            }
        });

        orderRepository.findByUserId(user.getId(), PageRequest.of(0, 25,
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getItems)
                .filter(items -> items != null)
                .flatMap(List::stream)
                .map(OrderItem::getProduct)
                .filter(product -> product != null)
                .forEach(product -> addProductSignals(product, purchasedCategories, interestTerms));

        return new PreferenceSignals(wishlistProductIds, normalizeSet(wishlistCategories),
                normalizeSet(purchasedCategories), normalizeSet(interestTerms));
    }

    private void addProductSignals(Product product, Set<String> categories, Set<String> terms) {
        Category category = product.getCategory();
        if (category != null && category.getName() != null && !category.getName().isBlank()) {
            categories.add(category.getName().trim());
        }
        if (product.getName() != null) {
            words(product.getName()).stream().filter(word -> word.length() >= 3 && !STOP_WORDS.contains(word))
                    .forEach(terms::add);
        }
    }

    private Specification<Product> buildSpecification(List<String> searchTerms, BigDecimal minBudget, BigDecimal maxBudget) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThan(root.get("stock"), 0));
            if (minBudget != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minBudget));
            if (maxBudget != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxBudget));

            if (!searchTerms.isEmpty()) {
                Join<Product, Category> category = root.join("category", JoinType.LEFT);
                List<Predicate> matches = new ArrayList<>();
                for (String term : searchTerms) {
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

    private ProductRecommendationDTO toRecommendation(Product product, ShoppingIntentDTO intent,
                                                       UserShoppingPreference preference, PreferenceSignals signals,
                                                       BigDecimal minBudget, BigDecimal maxBudget) {
        String categoryName = product.getCategory() == null ? null : product.getCategory().getName();
        String searchableText = normalize(String.join(" ", safe(product.getName()), safe(product.getDescription()), safe(categoryName)));
        List<String> required = safe(intent.requiredFeatures());
        List<String> matched = required.stream().filter(feature -> containsNormalized(searchableText, feature)).toList();
        List<String> unverified = required.stream().filter(feature -> !containsNormalized(searchableText, feature)).toList();

        int score = phraseScore(searchableText, intent.productType(), 10);
        score += phraseScore(searchableText, intent.category(), 6);
        score += phraseScore(searchableText, intent.useCase(), 3);

        for (String keyword : safe(intent.searchKeywords())) {
            score += phraseScore(searchableText, keyword, 3);
        }

        for (String feature : required) {
            if (containsNormalized(searchableText, feature)) {
                score += 6;
            }
        }

        // Preferences from the current shopping message
        for (String intentPreference : safe(intent.preferences())) {
            if (containsNormalized(searchableText, intentPreference)) {
                score += 2;
            }
        }

        // Saved customer category preferences
        for (String preferredCategory : safe(preference.getPreferredCategories())) {
            if (sameNormalized(categoryName, preferredCategory)) {
                score += 10;
            }
        }

        // Saved customer keyword preferences
        for (String preferredKeyword : safe(preference.getPreferredKeywords())) {
            if (containsNormalized(searchableText, preferredKeyword)) {
                score += 5;
            }
        }
        if (categoryName != null && signals.wishlistCategories().contains(normalize(categoryName))) score += 6;
        if (categoryName != null && signals.purchasedCategories().contains(normalize(categoryName))) score += 5;
        long interestHits = words(searchableText).stream().filter(signals.interestTerms()::contains).distinct().count();
        score += (int) Math.min(8, interestHits * 2);
        if (product.getId() != null && signals.wishlistProductIds().contains(product.getId())) score -= 3;
        if (maxBudget != null && product.getPrice() != null) {
            BigDecimal headroom = maxBudget.subtract(product.getPrice());
            if (headroom.signum() >= 0) score += headroom.compareTo(maxBudget.multiply(new BigDecimal("0.15"))) <= 0 ? 2 : 1;
        }

        List<String> reasons = new ArrayList<>();
        if (intent.productType() != null && !intent.productType().isBlank()) reasons.add("matches your current request");
        if (minBudget != null || maxBudget != null) reasons.add("fits your active budget range");
        if (product.getStock() != null && product.getStock() > 0) reasons.add("currently in stock");
        if (!matched.isEmpty()) reasons.add("catalog text mentions requested features: " + String.join(", ", matched));
        if (categoryName != null && signals.wishlistCategories().contains(normalize(categoryName))) reasons.add("matches a category in your wishlist");
        if (categoryName != null && signals.purchasedCategories().contains(normalize(categoryName))) reasons.add("matches a category from your previous orders");
        if (preference.getPreferredCategories().stream().anyMatch(c -> sameNormalized(c, categoryName))) reasons.add("matches a category you saved as a preference");
        if (preference.getPreferredKeywords().stream().anyMatch(k -> containsNormalized(searchableText, k))) reasons.add("matches your saved interests");
        String reason = reasons.isEmpty() ? "Matched your available catalog and personalization profile; review specifications before purchase."
                : "Recommended because it is " + String.join("; ", reasons) + ".";
        if (!unverified.isEmpty()) reason += " The catalog does not verify these requested features: " + String.join(", ", unverified) + ".";
        return new ProductRecommendationDTO(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getStock(), product.getImageUrl(), categoryName, Math.max(0, score), matched, unverified, reason);
    }

    private static BigDecimal effectiveMinBudget(ShoppingIntentDTO intent, UserShoppingPreference pref) {
        return intent.minBudget() != null ? intent.minBudget() : pref.getMinBudget();
    }

    private static BigDecimal effectiveMaxBudget(ShoppingIntentDTO intent, UserShoppingPreference pref) {
        return intent.maxBudget() != null ? intent.maxBudget() : pref.getMaxBudget();
    }

    private static List<String> intentTerms(ShoppingIntentDTO intent) {
        List<String> source = new ArrayList<>();
        source.add(intent.productType()); source.add(intent.category()); source.add(intent.useCase());
        source.addAll(safe(intent.searchKeywords())); source.addAll(safe(intent.requiredFeatures())); source.addAll(safe(intent.preferences()));
        return normalizeTerms(source);
    }

    private static List<String> normalizeTerms(List<String> source) {
        return source.stream().filter(s -> s != null && !s.isBlank()).flatMap(s -> words(s).stream())
                .filter(term -> term.length() >= 3 && !STOP_WORDS.contains(term)).distinct().limit(20).toList();
    }

    private static List<String> words(String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(normalize(value).split("\\s+"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static boolean containsNormalized(String haystack, String needle) {
        String normalizedNeedle = normalize(needle);
        return !normalizedNeedle.isEmpty() && ( (" " + haystack + " ").contains(" " + normalizedNeedle + " ") || haystack.contains(normalizedNeedle));
    }

    private static boolean sameNormalized(String first, String second) {
        return !normalize(first).isEmpty() && normalize(first).equals(normalize(second));
    }

    private static int phraseScore(String searchableText, String phrase, int points) {
        if (phrase == null || phrase.isBlank()) return 0;
        String normalizedPhrase = normalize(phrase);
        if (normalizedPhrase.isEmpty()) return 0;
        if (searchableText.contains(normalizedPhrase)) return points;
        List<String> tokens = words(normalizedPhrase);
        long hits = tokens.stream().filter(token -> searchableText.contains(token)).count();
        if (hits == 0) return 0;
        return (int) Math.max(1, Math.round(points * (hits / (double) tokens.size())));
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static List<String> safe(List<String> values) { return values == null ? List.of() : values.stream().filter(v -> v != null && !v.isBlank()).toList(); }
    private static Set<String> safe(Set<String> values) { return values == null ? Set.of() : values.stream().filter(v -> v != null && !v.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new)); }
    private static Set<String> normalizeSet(Set<String> values) { return values.stream().map(PersonalizedRecommendationService::normalize).filter(v -> !v.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new)); }

    private record PreferenceSignals(Set<Long> wishlistProductIds, Set<String> wishlistCategories,
                                     Set<String> purchasedCategories, Set<String> interestTerms) { }

}
