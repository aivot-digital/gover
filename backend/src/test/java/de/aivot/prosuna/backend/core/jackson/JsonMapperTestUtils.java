package de.aivot.prosuna.backend.core.jackson;

import de.aivot.prosuna.backend.JsonMapperConfiguration;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

public final class JsonMapperTestUtils {
    private JsonMapperTestUtils() {
    }

    public static JsonMapper createMapper() {
        var builder = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
        new JsonMapperConfiguration().customJacksonSerializers().customize(builder);
        return builder.build();
    }

    public static JsonMapper installMapper() {
        var previous = (JsonMapper) ReflectionTestUtils.getField(JsonMapperFactory.class, "mapper");
        ReflectionTestUtils.setField(JsonMapperFactory.class, "mapper", createMapper());
        return previous;
    }

    public static void restoreMapper(JsonMapper mapper) {
        ReflectionTestUtils.setField(JsonMapperFactory.class, "mapper", mapper);
    }
}
