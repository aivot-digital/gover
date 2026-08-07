package de.aivot.gover.backend.plugins.core.v1.javascript;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.gover.backend.dataObject.filters.DataObjectItemFilter;
import de.aivot.gover.backend.dataObject.services.DataObjectItemService;
import de.aivot.gover.backend.dataObject.services.DataObjectSchemaService;
import de.aivot.gover.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.core.CorePlugin;
import de.aivot.gover.backend.utils.IsoTimestampUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class provides JavaScript functions for retrieving data objects.
 * The functions are exposed to the JavaScript environment through the GraalVM Polyglot API.
 */
@Component
public class DataObjectJavascriptV1 implements JavascriptFunctionProvider {
    private final DataObjectSchemaService dataObjectSchemaService;
    private final DataObjectItemService dataObjectItemService;

    @Autowired
    public DataObjectJavascriptV1(DataObjectSchemaService dataObjectSchemaService,
                                  @Lazy DataObjectItemService dataObjectItemService) {
        this.dataObjectSchemaService = dataObjectSchemaService;
        this.dataObjectItemService = dataObjectItemService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "data_objects";
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
        return "Datenobjekte";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Datenobjekte.";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "getSchema(schemaKey: string | null): Record<string, any> | null;",
                "list(schemaKey: string | null, fieldFilter: Array<{path: string; operator: string; value: string}> | null): Array<Record<string, any>>;",
                "create(schemaKey: string | null, value: Record<string, any> | null): Record<string, any> | null;",
                "retrieve(schemaKey: string | null, itemId: string | null): Record<string, any> | null;",
                "update(schemaKey: string | null, itemId: string | null, value: Record<string, any> | null): Record<string, any> | null;"
        };
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
    public ProxyArray list(@Nullable String dataObjectSchemaKey, @Nullable Value fieldFilterValue) {
        if (dataObjectSchemaKey == null) {
            return ProxyArray
                    .fromList(List.of());
        }

        var filter = new DataObjectItemFilter()
                .setSchemaKey(dataObjectSchemaKey);

        var fieldFilter = valueToListOfMaps(fieldFilterValue);
        if (fieldFilter != null) {
            var fields = new LinkedList<DataObjectItemFilter.DataObjectFilterField>();

            for (var fieldMap : fieldFilter) {
                var field = new DataObjectItemFilter.DataObjectFilterField();
                field.setPath(valueToString(fieldMap.get("path")));
                field.setOperator(valueToString(fieldMap.get("operator")));
                field.setValue(valueToString(fieldMap.get("value")));
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
                .map(DataObjectJavascriptV1::getItemData)
                .toList();

        return JavascriptEngine
                .collectionToProxyArray(items);
    }

    @HostAccess.Export
    public ProxyObject create(@Nullable String schemaKey, @Nullable Value value) {
        if (schemaKey == null) {
            return null;
        }

        var valueMap = valueToMap(value);
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
    public ProxyObject update(@Nullable String schemaKey, @Nullable String itemId, @Nullable Value value) {
        if (schemaKey == null) {
            return null;
        }

        if (itemId == null) {
            return null;
        }

        var valueMap = valueToMap(value);
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
        data.put("$created", IsoTimestampUtils.toOffsetString(entity.getCreated()));
        data.put("$updated", IsoTimestampUtils.toOffsetString(entity.getUpdated()));
        return data;
    }

    @Nullable
    private static List<Map<String, Object>> valueToListOfMaps(@Nullable Value value) {
        if (isNullValue(value) || !value.hasArrayElements()) {
            return null;
        }

        var list = new ArrayList<Map<String, Object>>();
        for (var i = 0L; i < value.getArraySize(); i++) {
            var item = valueToMap(value.getArrayElement(i));
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    @Nullable
    private static Map<String, Object> valueToMap(@Nullable Value value) {
        if (isNullValue(value) || !value.hasMembers()) {
            return null;
        }

        var map = new HashMap<String, Object>();
        for (var key : value.getMemberKeys()) {
            map.put(key, valueToJavaObject(value.getMember(key)));
        }
        return map;
    }

    @Nullable
    private static Object valueToJavaObject(@Nullable Value value) {
        if (isNullValue(value)) {
            return null;
        }

        if (value.hasArrayElements()) {
            var list = new ArrayList<>();
            for (var i = 0L; i < value.getArraySize(); i++) {
                list.add(valueToJavaObject(value.getArrayElement(i)));
            }
            return list;
        }

        if (value.isString()) {
            return value.asString();
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.asDouble();
        }

        if (value.hasMembers()) {
            return valueToMap(value);
        }

        return value.as(Object.class);
    }

    @Nullable
    private static String valueToString(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isNullValue(@Nullable Value value) {
        return value == null || value.isNull();
    }
}
