package com.ecommerce.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.User;
import com.ecommerce.auth.UserRepository;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
                                         UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationPreference getOrCreate(Long userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.getReferenceById(userId);
            NotificationPreference preference = new NotificationPreference(user);
            return preferenceRepository.save(preference);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationPreference update(Long userId,
                                         boolean realtimeOrderStatusEnabled,
                                         boolean orderInvoiceEmailEnabled) {
        NotificationPreference preference = getOrCreate(userId);
        preference.setRealtimeOrderStatusEnabled(realtimeOrderStatusEnabled);
        preference.setOrderInvoiceEmailEnabled(orderInvoiceEmailEnabled);
        return preferenceRepository.save(preference);
    }
}
