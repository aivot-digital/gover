package de.aivot.prosuna.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "de.aivot.prosuna.backend.asset.repositories",
        "de.aivot.prosuna.backend.audit.repositories",
        "de.aivot.prosuna.backend.codeLists.repositories",
        "de.aivot.prosuna.backend.config.repositories",
        "de.aivot.prosuna.backend.dataObject.repositories",
        "de.aivot.prosuna.backend.department.repositories",
        "de.aivot.prosuna.backend.destination.repositories",
        "de.aivot.prosuna.backend.form.repositories",
        "de.aivot.prosuna.backend.identity.repositories",
        "de.aivot.prosuna.backend.payment.repositories",
        "de.aivot.prosuna.backend.permissions.repositories",
        "de.aivot.prosuna.backend.preset.repositories",
        "de.aivot.prosuna.backend.process.repositories",
        "de.aivot.prosuna.backend.customLink.repositories",
        "de.aivot.prosuna.backend.search.repositories",
        "de.aivot.prosuna.backend.secrets.repositories",
        "de.aivot.prosuna.backend.storage.repositories",
        "de.aivot.prosuna.backend.submission.repositories",
        "de.aivot.prosuna.backend.teams.repositories",
        "de.aivot.prosuna.backend.theme.repositories",
        "de.aivot.prosuna.backend.user.repositories",
        "de.aivot.prosuna.backend.userRoles.repositories",
})
@EnableRedisRepositories(basePackages = {
        "de.aivot.prosuna.backend.form.cache.repositories",
        "de.aivot.prosuna.backend.identity.cache.repositories",
})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ProsunaBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProsunaBackendApplication.class, args);
    }
}
