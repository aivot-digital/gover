package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> getByEmail(String email);

    boolean existsByEmail(String email);

    Collection<User> findAllByOrderByEmail();
    
    Collection<User> findAllByAdminOrderByEmail(Boolean admin);

    boolean existsByAdminIsTrue();
}
