package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.User;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Iterable<User> findByRole(UserRole role);
    boolean existsByRole(UserRole role);
}