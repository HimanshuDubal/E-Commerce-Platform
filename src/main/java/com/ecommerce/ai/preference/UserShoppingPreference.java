package com.ecommerce.ai.preference;

import com.ecommerce.auth.User;
import com.ecommerce.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "user_shopping_preferences",
       uniqueConstraints = @UniqueConstraint(name = "uk_shopping_preference_user", columnNames = "user_id"))
public class UserShoppingPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean personalizationEnabled = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal minBudget;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxBudget;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_preferred_categories", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "category_name", nullable = false, length = 100)
    private Set<String> preferredCategories = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_preferred_keywords", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "keyword", nullable = false, length = 100)
    private Set<String> preferredKeywords = new LinkedHashSet<>();

    public UserShoppingPreference() {
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isPersonalizationEnabled() {
        return personalizationEnabled;
    }

    public void setPersonalizationEnabled(boolean personalizationEnabled) {
        this.personalizationEnabled = personalizationEnabled;
    }

    public BigDecimal getMinBudget() {
        return minBudget;
    }

    public void setMinBudget(BigDecimal minBudget) {
        this.minBudget = minBudget;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public Set<String> getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(Set<String> preferredCategories) {
        this.preferredCategories.clear();
        if (preferredCategories != null) {
            this.preferredCategories.addAll(preferredCategories);
        }
    }

    public Set<String> getPreferredKeywords() {
        return preferredKeywords;
    }

    public void setPreferredKeywords(Set<String> preferredKeywords) {
        this.preferredKeywords.clear();
        if (preferredKeywords != null) {
            this.preferredKeywords.addAll(preferredKeywords);
        }
    }
}
