package de.aivot.prosuna.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagementServerConfigurationTest {
    @Test
    void shouldUseSeparateInternalPortByDefault() throws IOException {
        var environment = applicationEnvironment();

        var properties = bind(environment);

        assertEquals(9090, properties.getPort());
        assertEquals("0.0.0.0", properties.getAddress().getHostAddress());
        assertEquals(ManagementPortType.DIFFERENT, ManagementPortType.get(environment));
    }

    @Test
    void shouldUsePortAndAddressFromVariables() throws IOException {
        var environment = applicationEnvironment()
                .withProperty("PROSUNA_MANAGEMENT_SERVER_PORT", "9191")
                .withProperty("PROSUNA_MANAGEMENT_SERVER_ADDRESS", "127.0.0.1");

        var properties = bind(environment);

        assertEquals(9191, properties.getPort());
        assertEquals("127.0.0.1", properties.getAddress().getHostAddress());
        assertEquals(ManagementPortType.DIFFERENT, ManagementPortType.get(environment));
    }

    private static ManagementServerProperties bind(MockEnvironment environment) {
        return Binder.get(environment).bindOrCreate("management.server", ManagementServerProperties.class);
    }

    private static MockEnvironment applicationEnvironment() throws IOException {
        var environment = new MockEnvironment();
        // Spring Boot binds the address to an InetAddress through its application conversion service.
        environment.setConversionService(new ApplicationConversionService());
        for (PropertySource<?> source : new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))) {
            environment.getPropertySources().addLast(source);
        }
        return environment;
    }
}
