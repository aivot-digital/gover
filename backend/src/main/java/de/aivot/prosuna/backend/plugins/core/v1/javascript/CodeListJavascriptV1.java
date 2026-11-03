package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.services.CodeListService;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides JavaScript functions for retrieving code lists.
 * The functions are exposed to the JavaScript environment through the GraalVM Polyglot API.
 */
@Component
public class CodeListJavascriptV1 implements JavascriptFunctionProvider {
    private final CodeListService codeListService;
    private final CodeListElementOptionsService codeListElementOptionsService;

    @Autowired
    public CodeListJavascriptV1(@Lazy CodeListService codeListService,
                                @Lazy CodeListElementOptionsService codeListElementOptionsService) {
        this.codeListService = codeListService;
        this.codeListElementOptionsService = codeListElementOptionsService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "code_lists";
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
        return "Codelisten";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Dieses Paket enthält Funktionen für Codelisten.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Ermöglicht den Zugriff auf in Prosuna hinterlegte Codelisten aus JavaScript-Ausdrücken.

                Eine Codeliste kann entweder als vollständige Liste ihrer Einträge oder als Auswahloptionen aus einer Wert- und einer Beschriftungsspalte geladen werden. Spalten werden über ihren Spaltenschlüssel angegeben. Ohne Angabe wird die Schlüsselspalte der Codeliste verwendet. Dadurch lassen sich dieselben gepflegten Werte in Skripten, Ausdrücken und Oberflächen wiederverwenden.
                """;
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "getItems(codeListKey: string | null): Array<Record<string, any>>;",
                "getOptions(codeListKey: string | null, valueColumnKey?: string | null, labelColumnKey?: string | null): Array<{value: string; label: string}>;"
        };
    }

    @HostAccess.Export
    public ProxyArray getItems(@Nullable String codeListKey) {
        if (codeListKey == null || codeListKey.isBlank()) {
            return ProxyArray.fromArray();
        }

        var codeList = codeListService
                .retrieve(codeListKey)
                .orElse(null);
        if (codeList == null) {
            return ProxyArray.fromArray();
        }

        List<CodeListItemEntity> items;
        try {
            items = codeListService.listAllItems(codeListKey);
        } catch (ResponseException e) {
            return ProxyArray.fromArray();
        }

        var rows = items
                .stream()
                .map(item -> getItemData(codeList, item))
                .toList();

        return JavascriptEngine.collectionToProxyArray(rows);
    }

    @HostAccess.Export
    public ProxyArray getOptions(@Nullable String codeListKey) {
        return getOptions(codeListKey, null, null);
    }

    @HostAccess.Export
    public ProxyArray getOptions(@Nullable String codeListKey, @Nullable String valueColumnKey) {
        return getOptions(codeListKey, valueColumnKey, null);
    }

    /** Without a value column the key column is used; without a label column the value column is used. */
    @HostAccess.Export
    public ProxyArray getOptions(@Nullable String codeListKey,
                                 @Nullable String valueColumnKey,
                                 @Nullable String labelColumnKey) {
        if (codeListKey == null || codeListKey.isBlank()) {
            return ProxyArray.fromArray();
        }

        var codeList = codeListService
                .retrieve(codeListKey)
                .orElse(null);
        if (codeList == null) {
            return ProxyArray.fromArray();
        }

        var valueColumn = StringUtils.toNullableTrimmedString(valueColumnKey);
        if (valueColumn == null) {
            valueColumn = codeList.getKeyColumnKey();
        }
        var labelColumn = StringUtils.toNullableTrimmedString(labelColumnKey);
        if (labelColumn == null) {
            labelColumn = valueColumn;
        }
        if (valueColumn == null) {
            return ProxyArray.fromArray();
        }

        List<Map<String, String>> options;
        try {
            options = codeListElementOptionsService
                    .listOptions(codeListKey, valueColumn, labelColumn, null)
                    .stream()
                    .map(option -> Map.of(
                            "value", option.getValue(),
                            "label", option.getLabel()
                    ))
                    .toList();
        } catch (ResponseException e) {
            return ProxyArray.fromArray();
        }

        return JavascriptEngine.collectionToProxyArray(options);
    }

    @Nonnull
    private static Map<String, Object> getItemData(@Nonnull CodeListEntity codeList, @Nonnull CodeListItemEntity item) {
        var data = new LinkedHashMap<String, Object>();
        for (var mapping : codeList.getColumnMappings()) {
            if (mapping.index() != null) {
                data.put(mapping.key(), item.cellAt(mapping.index()));
            }
        }

        var keyColumnIndex = codeList
                .findColumnMapping(codeList.getKeyColumnKey())
                .map(mapping -> mapping.index())
                .orElse(null);
        data.put("$id", item.getId());
        data.put("$value", keyColumnIndex == null ? null : item.cellAt(keyColumnIndex));
        data.put("$created", item.getCreated());
        data.put("$updated", item.getUpdated());

        return data;
    }
}
