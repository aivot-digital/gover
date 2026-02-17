package de.aivot.GoverBackend.plugins.core.v1.javascript;

import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.xrepository.services.XRepositoryCodeListService;
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
public class XRepositoryCodelistJavascript implements JavascriptFunctionProvider {
    private final XRepositoryCodeListService codeListService;

    @Autowired
    public XRepositoryCodelistJavascript(XRepositoryCodeListService codeListService) {
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
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "XRepository Codelisten";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Codelisten aus den XRepositories.";
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

        return JavascriptEngine
                .collectionToProxyArray(codeList);
    }
}
