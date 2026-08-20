package de.aivot.prosuna.backend.codeLists.services;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.enums.CodeListSourceType;
import de.aivot.prosuna.backend.codeLists.filters.CodeListFilter;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugin.models.Plugin;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CodeListStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CodeListStartupService.class);

    private final Set<String> existingPluginKeys;
    private final CodeListService codeListService;

    @Autowired
    public CodeListStartupService(List<Plugin> plugins, List<PluginComponent> pluginComponents, CodeListService codeListService) {
        this.existingPluginKeys = plugins
                .stream()
                .map(Plugin::getKey)
                .collect(Collectors.toSet());

        this.codeListService = codeListService;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        var pluginCodeListsFilter = CodeListFilter
                .create()
                .setSourceType(CodeListSourceType.Plugin);

        Page<CodeListEntity> codeLists;
        try {
            codeLists = codeListService
                    .list(pluginCodeListsFilter);
        } catch (ResponseException e) {
            throw new RuntimeException(e);
        }

        for (var codeList : codeLists) {
            var sourceRefPluginKey = codeList
                    .getSourceRef()
                    .replaceAll("\\.[^.]*$", "");

            if (existingPluginKeys.contains(sourceRefPluginKey)) {
                continue;
            }

            codeList
                    .setSourceType(CodeListSourceType.Manual)
                    .setSourceRef("");

            try {
                codeListService.update(codeList.getKey(), codeList);
            } catch (ResponseException e) {
                throw new RuntimeException(e);
            }

            logger.warn(
                    "Das verwaltende Plugin für die Codeliste {} ist nicht mehr verfügbar. " +
                            "Die Codeliste wird nun als manuell verwaltet markiert.",
                    StringUtils.quote(codeList.getName())
            );
        }
    }
}
