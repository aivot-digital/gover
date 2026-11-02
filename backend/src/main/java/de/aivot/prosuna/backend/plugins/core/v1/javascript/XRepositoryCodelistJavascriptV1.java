package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.xrepository.services.XRepositoryCodeListService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * This class provides JavaScript functions for retrieving data objects.
 * The functions are exposed to the JavaScript environment through the GraalVM Polyglot API.
 */
@Component
public class XRepositoryCodelistJavascriptV1 implements JavascriptFunctionProvider {
    private final XRepositoryCodeListService codeListService;

    @Autowired
    public XRepositoryCodelistJavascriptV1(XRepositoryCodeListService codeListService) {
        this.codeListService = codeListService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "xrp_codelists";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "XRepository Codelisten";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Dieses Paket enthält Funktionen für Codelisten aus den XRepositories.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Bindet über eine URN referenzierte Codelisten aus den XRepositories in JavaScript-Ausdrücke ein.

                Die Werte einer Codeliste können entweder vollständig oder als direkt verwendbare Auswahloptionen geladen werden. Damit stehen zentral veröffentlichte Verwaltungsstandards auch in Skripten und dynamischen Ausdrücken zur Verfügung.
                """;
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "getValues(urn: string | null): Array<Record<string, any>>;",
                "getOptions(urn: string | null): Array<{value: string; label: string}>;"
        };
    }

    @HostAccess.Export
    public ProxyArray getValues(@Nullable String urn) {
        if (urn == null) {
            return ProxyArray.fromArray();
        }

        List<Map<String, String>> codeList;
        try {
            codeList = codeListService
                    .getReducedCodeList(urn);
        } catch (ResponseException e) {
            return ProxyArray.fromArray();
        }

        if (codeList == null) {
            return ProxyArray.fromArray();
        }

        return JavascriptEngine
                .collectionToProxyArray(codeList);
    }

    @HostAccess.Export
    public ProxyArray getOptions(@Nullable String urn) {
        if (urn == null) {
            return ProxyArray.fromArray();
        }

        List<RadioInputElementOption> codeList;
        try {
            codeList = codeListService
                    .getRadioFieldOptionCodeList(urn);
        } catch (ResponseException e) {
            return ProxyArray.fromArray();
        }

        if (codeList == null) {
            return ProxyArray.fromArray();
        }

        var items = codeList
                .stream()
                .map(option -> Map.of(
                        "value", option.getValue(),
                        "label", option.getLabel()
                ))
                .toList();

        return JavascriptEngine
                .collectionToProxyArray(items);
    }
}
