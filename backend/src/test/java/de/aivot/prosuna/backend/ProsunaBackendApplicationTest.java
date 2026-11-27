package de.aivot.prosuna.backend;

import de.aivot.prosuna.backend.customLink.repositories.CustomLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProsunaBackendApplicationTest {
    @Test
    void jpaRepositoryScanShouldIncludeCustomLinks() {
        var configuration = ProsunaBackendApplication.class.getAnnotation(EnableJpaRepositories.class);

        assertTrue(List.of(configuration.basePackages()).contains(CustomLinkRepository.class.getPackageName()));
    }
}
