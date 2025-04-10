package de.aivot.GoverBackend.system.controllers;

import de.aivot.GoverBackend.models.config.GoverConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SystemController {
    private final GoverConfig goverConfig;

    @Autowired
    public SystemController(
            GoverConfig goverConfig
    ) {
        this.goverConfig = goverConfig;
    }

    @GetMapping("/api/public/system/sentry-dsn")
    public List<String> getSentryDns() {
        return List.of(goverConfig.getSentryWebApp());
    }

    @GetMapping("/api/system/file-extensions")
    public List<String> getFileExtensions() {
        return goverConfig.getFileExtensions();
    }
}
