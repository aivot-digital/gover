package de.aivot.gover.backend.plugins.core.v1.javascript;

import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import de.aivot.gover.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.gover.backend.codeLists.services.CodeListService;
import de.aivot.gover.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.core.CorePlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public CodeListJavascriptV1(CodeListService codeListService) {
        this.codeListService = codeListService;
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
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Codelisten.";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "getItems(codeListId: number | null): Array<Record<string, any>>;",
                "getOptions(codeListId: number | null): Array<{value: string; label: string}>;"
        };
    }

    @HostAccess.Export
    public ProxyArray getItems(@Nullable Integer codeListId) {
        if (codeListId == null) {
            return ProxyArray.fromArray();
        }

        var codeList = codeListService
                .retrieve(codeListId)
                .orElse(null);
        if (codeList == null) {
            return ProxyArray.fromArray();
        }

        List<VCodeListItemEntity> items;
        try {
            items = codeListService.listAllItems(codeListId);
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
    public ProxyArray getOptions(@Nullable Integer codeListId) {
        if (codeListId == null) {
            return ProxyArray.fromArray();
        }

        List<VCodeListItemEntity> items;
        try {
            items = codeListService.listAllItems(codeListId);
        } catch (ResponseException e) {
            return ProxyArray.fromArray();
        }

        var options = items
                .stream()
                .map(item -> Map.of(
                        "value", item.getValue(),
                        "label", item.getLabel()
                ))
                .toList();

        return JavascriptEngine.collectionToProxyArray(options);
    }

    @Nonnull
    private static Map<String, Object> getItemData(@Nonnull CodeListEntity codeList, @Nonnull VCodeListItemEntity item) {
        var data = new LinkedHashMap<String, Object>();
        var columns = codeList.getColumns();
        var values = item.getColumns();

        for (var i = 0; i < columns.size(); i++) {
            data.put(columns.get(i), i < values.size() ? values.get(i) : null);
        }

        data.put("$id", item.getId());
        data.put("$value", item.getValue());
        data.put("$label", item.getLabel());
        data.put("$created", item.getCreated().toString());
        data.put("$updated", item.getUpdated().toString());

        return data;
    }
}
