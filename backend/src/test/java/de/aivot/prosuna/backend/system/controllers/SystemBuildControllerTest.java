package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.system.dtos.SystemBuildDTO;
import de.aivot.prosuna.backend.system.properties.BuildProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemBuildControllerTest {
    @Test
    void getBuildReturnsTheBackendBuildProperties() {
        var properties = new BuildProperties();
        properties.setBuildVersion("5.0.0");
        properties.setBuildNumber("42");
        properties.setBuildTimestamp("2026-11-27T10:00:00Z");

        var build = new SystemBuildController(properties).getBuild();

        assertEquals(new SystemBuildDTO("5.0.0", "42", "2026-11-27T10:00:00Z"), build);
    }
}
