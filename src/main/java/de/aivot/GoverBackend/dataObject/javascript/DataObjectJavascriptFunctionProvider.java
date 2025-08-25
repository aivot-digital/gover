package de.aivot.GoverBackend.dataObject.javascript;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.GoverBackend.dataObject.filters.DataObjectItemFilter;
import de.aivot.GoverBackend.dataObject.services.DataObjectItemService;
import de.aivot.GoverBackend.dataObject.services.DataObjectSchemaService;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class provides JavaScript functions for retrieving data objects.
 * The functions are exposed to the JavaScript environment through the GraalVM Polyglot API.
 */
@Component
public class DataObjectJavascriptFunctionProvider implements JavascriptFunctionProvider {
    private final DataObjectSchemaService dataObjectSchemaService;
    private final DataObjectItemService dataObjectItemService;

    @Autowired
    public DataObjectJavascriptFunctionProvider(DataObjectSchemaService dataObjectSchemaService,
                                                @Lazy DataObjectItemService dataObjectItemService) {
        this.dataObjectSchemaService = dataObjectSchemaService;
        this.dataObjectItemService = dataObjectItemService;
    }


    @Override
    public String getPackageName() {
        return "_data_objects";
    }

    @Override
    public String getLabel() {
        return "Datenobjekte";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Datenobjekte.";
    }

    @HostAccess.Export
    public ProxyObject getSchema(@Nullable String schemaKey) {
        if (schemaKey == null) {
            return null;
        }

        var dataObjectSchema = dataObjectSchemaService
                .retrieve(schemaKey)
                .orElse(null);

        if (dataObjectSchema == null) {
            return null;
        }

        var schemaMap = new ObjectMapper()
                .convertValue(dataObjectSchema.getSchema(), Map.class);

        return JavascriptEngine
                .mapToProxyObject(schemaMap);
    }

    @HostAccess.Export
    public ProxyArray list(@Nullable String dataObjectSchemaKey, @Nullable List<Map<String, Object>> fieldFilter) {
        if (dataObjectSchemaKey == null) {
            return ProxyArray
                    .fromList(List.of());
        }

        var filter = new DataObjectItemFilter()
                .setSchemaKey(dataObjectSchemaKey);

        if (fieldFilter != null) {
            var fields = new LinkedList<DataObjectItemFilter.DataObjectFilterField>();

            for (var fieldMap : fieldFilter) {
                var field = new DataObjectItemFilter.DataObjectFilterField();
                field.setPath((String) fieldMap.get("path"));
                field.setOperator((String) fieldMap.get("operator"));
                field.setOperator((String) fieldMap.get("value"));
                fields.add(field);
            }

            filter.setDataFields(fields);
        }

        List<DataObjectItemEntity> page;
        try {
            page = dataObjectItemService
                    .list(filter)
                    .getContent();
        } catch (ResponseException e) {
            return ProxyArray
                    .fromList(List.of());
        }

        var items = page
                .stream()
                .map(DataObjectJavascriptFunctionProvider::getItemData)
                .toList();

        return JavascriptEngine
                .collectionToProxyArray(items);
    }

    @HostAccess.Export
    public ProxyObject create(@Nullable String schemaKey, @Nullable Map<String, Object> valueMap) {
        if (schemaKey == null) {
            return null;
        }

        if (valueMap == null) {
            valueMap = new HashMap<>();
        }

        var dataObjectSchema = dataObjectSchemaService
                .retrieve(schemaKey)
                .orElse(null);

        if (dataObjectSchema == null) {
            return null;
        }

        var newEntity = new DataObjectItemEntity()
                .setData(valueMap)
                .setSchemaKey(schemaKey);

        DataObjectItemEntity createdEntity;
        try {
            createdEntity = dataObjectItemService
                    .create(newEntity);
        } catch (ResponseException e) {
            return null;
        }

        return JavascriptEngine
                .mapToProxyObject(getItemData(createdEntity));
    }

    @HostAccess.Export
    public ProxyObject retrieve(@Nullable String schemaKey, @Nullable String dataObjectItemId) {
        if (schemaKey == null) {
            return null;
        }

        if (dataObjectItemId == null) {
            return null;
        }

        var id = new DataObjectItemEntityId(schemaKey, dataObjectItemId);

        DataObjectItemEntity item;
        try {
            item = dataObjectItemService
                    .retrieve(id)
                    .orElse(null);
        } catch (ResponseException e) {
            return null;
        }

        if (item == null) {
            return null;
        }

        return JavascriptEngine
                .mapToProxyObject(getItemData(item));
    }

    @HostAccess.Export
    public ProxyObject update(@Nullable String schemaKey, @Nullable String itemId, @Nullable Map<String, Object> valueMap) {
        if (schemaKey == null) {
            return null;
        }

        if (itemId == null) {
            return null;
        }

        if (valueMap == null) {
            return null;
        }

        var dataObjectSchema = dataObjectSchemaService
                .retrieve(schemaKey)
                .orElse(null);

        if (dataObjectSchema == null) {
            return null;
        }

        var dataObjectItemId = new DataObjectItemEntityId(schemaKey, itemId);

        var updatedEntity = new DataObjectItemEntity()
                .setData(valueMap)
                .setSchemaKey(schemaKey);

        DataObjectItemEntity updatedItemEntity;
        try {
            updatedItemEntity = dataObjectItemService
                    .update(dataObjectItemId, updatedEntity);
        } catch (ResponseException e) {
            return null;
        }

        return JavascriptEngine
                .mapToProxyObject(getItemData(updatedItemEntity));
    }

    @Nonnull
    private static Map<String, Object> getItemData(@Nonnull DataObjectItemEntity entity) {
        var data = entity.getData();
        data.put("$id", entity.getId());
        data.put("$created", entity.getCreated().format(DateTimeFormatter.ISO_DATE_TIME));
        data.put("$updated", entity.getUpdated().format(DateTimeFormatter.ISO_DATE_TIME));
        return data;
    }
}
