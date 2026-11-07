package org.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.models.UserSettings;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class UserSettingsRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<UserSettings> getByUsername(String username) {
        return Optional.ofNullable(entityManager.find(UserSettings.class, username));
    }

    @Transactional(transactionManager = "jpaTransactionManager")
    public void save(UserSettings settings) {
        entityManager.merge(settings);
    }
}
