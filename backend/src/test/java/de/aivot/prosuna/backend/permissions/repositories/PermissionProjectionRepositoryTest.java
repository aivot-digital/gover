package de.aivot.prosuna.backend.permissions.repositories;

import de.aivot.prosuna.backend.process.repositories.VUserProcessAccessPermissionsRepository;
import de.aivot.prosuna.backend.process.repositories.VUserProcessInstanceAccessPermissionsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:permission-projections",
        "spring.jpa.properties.hibernate.connection.username=sa",
        "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"
})
@ContextConfiguration(classes = PermissionProjectionRepositoryTest.JpaTestConfiguration.class)
class PermissionProjectionRepositoryTest {
    @Autowired
    private VUserDomainPermissionRepository domainPermissionRepository;

    @Autowired
    private VUserProcessAccessPermissionsRepository processPermissionRepository;

    @Autowired
    private VUserProcessInstanceAccessPermissionsRepository processInstancePermissionRepository;

    @Test
    void projectionQueriesAreValid() {
        assertNotNull(domainPermissionRepository);
        assertNotNull(processPermissionRepository);
        assertNotNull(processInstancePermissionRepository);
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan("de.aivot.prosuna.backend")
    @EnableJpaRepositories(basePackages = {
            "de.aivot.prosuna.backend.permissions.repositories",
            "de.aivot.prosuna.backend.process.repositories"
    })
    static class JpaTestConfiguration {
    }
}
