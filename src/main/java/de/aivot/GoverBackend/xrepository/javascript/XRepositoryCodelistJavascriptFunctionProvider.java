package de.aivot.GoverBackend.xrepository.javascript;

import de.aivot.GoverBackend.elements.models.elements.form.input.RadioFieldOption;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.xrepository.services.XRepositoryCodeListService;
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
public class XRepositoryCodelistJavascriptFunctionProvider implements JavascriptFunctionProvider {
    private final XRepositoryCodeListService codeListService;

    @Autowired
    public XRepositoryCodelistJavascriptFunctionProvider(XRepositoryCodeListService codeListService) {
        this.codeListService = codeListService;
    }


    @Override
    public String getPackageName() {
        return "_xrp_codelists";
    }

    @Override
    public String getLabel() {
        return "XRepository Codelisten";
    }

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

        List<RadioFieldOption> codeList;
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
