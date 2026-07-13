package de.aivot.gover.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "de.aivot.gover.backend.asset.repositories",
        "de.aivot.gover.backend.audit.repositories",
        "de.aivot.gover.backend.codeLists.repositories",
        "de.aivot.gover.backend.config.repositories",
        "de.aivot.gover.backend.dataObject.repositories",
        "de.aivot.gover.backend.department.repositories",
        "de.aivot.gover.backend.destination.repositories",
        "de.aivot.gover.backend.form.repositories",
        "de.aivot.gover.backend.identity.repositories",
        "de.aivot.gover.backend.payment.repositories",
        "de.aivot.gover.backend.permissions.repositories",
        "de.aivot.gover.backend.preset.repositories",
        "de.aivot.gover.backend.process.repositories",
        "de.aivot.gover.backend.providerLink.repositories",
        "de.aivot.gover.backend.search.repositories",
        "de.aivot.gover.backend.secrets.repositories",
        "de.aivot.gover.backend.storage.repositories",
        "de.aivot.gover.backend.submission.repositories",
        "de.aivot.gover.backend.teams.repositories",
        "de.aivot.gover.backend.theme.repositories",
        "de.aivot.gover.backend.user.repositories",
        "de.aivot.gover.backend.userRoles.repositories",
})
@EnableRedisRepositories(basePackages = {
        "de.aivot.gover.backend.form.cache.repositories",
        "de.aivot.gover.backend.identity.cache.repositories",
})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class GoverBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoverBackendApplication.class, args);
    }
}
