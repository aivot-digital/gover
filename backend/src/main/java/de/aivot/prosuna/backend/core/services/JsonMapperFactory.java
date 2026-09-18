package de.aivot.prosuna.backend.core.services;

import de.aivot.prosuna.backend.utils.SpringContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JsonMapperFactory {
    private static JsonMapper mapper;

    public static JsonMapper getInstance() {
        if (mapper == null) {
            mapper = SpringContext
                    .getBean(JsonMapper.class);
        }
        return mapper;
    }

    /**
     * @deprecated Use {@link #getInstance()} instead. This method is kept for backward compatibility and will be removed in future versions.
     */
    @Deprecated
    public static JsonMapper getNullPreservingInstance() {
        return mapper;
    }

    public static final class Utils {
        public static <T> List<T> convertToList(Object value, Class<T> elementType) {
            JsonMapper om = getInstance();
            List<T> res = new LinkedList<>();

            if (value instanceof Collection<?> cValue) {
                for (Object itemObj : cValue) {
                    var conv = om.convertValue(itemObj, elementType);
                    res.add(conv);
                }
            }

            return res;
        }

        public static Map<String, Object> convertToMap(Object value) {
            JsonMapper om = getInstance();
            return om.convertValue(value, Map.class);
        }

        public static Map<String, Object> convertToMapPreservingNulls(Object value) {
            JsonMapper om = getNullPreservingInstance();
            return om.convertValue(value, Map.class);
        }
    }
}
