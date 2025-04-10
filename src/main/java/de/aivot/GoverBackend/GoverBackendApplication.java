package de.aivot.GoverBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "de.aivot.GoverBackend.asset.repositories",
        "de.aivot.GoverBackend.config.repositories",
        "de.aivot.GoverBackend.department.repositories",
        "de.aivot.GoverBackend.destination.repositories",
        "de.aivot.GoverBackend.form.repositories",
        "de.aivot.GoverBackend.identity.repositories",
        "de.aivot.GoverBackend.payment.repositories",
        "de.aivot.GoverBackend.preset.repositories",
        "de.aivot.GoverBackend.providerLink.repositories",
        "de.aivot.GoverBackend.secrets.repositories",
        "de.aivot.GoverBackend.submission.repositories",
        "de.aivot.GoverBackend.theme.repositories",
        "de.aivot.GoverBackend.user.repositories",
})
@EnableRedisRepositories(basePackages = {
        "de.aivot.GoverBackend.form.cache.repositories",
        "de.aivot.GoverBackend.user.cache.repositories",
})
public class GoverBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoverBackendApplication.class, args);
    }
}
