package org.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.models.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class UserRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<User> getByUsername(String username){
        return Optional.ofNullable(
                entityManager
                        .createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                        .setParameter("username", username)
                        .getSingleResultOrNull()
        );
    }

    @Transactional(transactionManager = "jpaTransactionManager")
    public void create(User user){
        entityManager.persist(user);
    }
}
