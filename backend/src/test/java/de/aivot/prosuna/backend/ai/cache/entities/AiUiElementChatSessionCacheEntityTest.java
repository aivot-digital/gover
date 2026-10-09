package de.aivot.prosuna.backend.ai.cache.entities;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.convert.MappingRedisConverter;
import org.springframework.data.redis.core.convert.RedisData;
import org.springframework.data.redis.core.mapping.RedisMappingContext;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AiUiElementChatSessionCacheEntityTest {
    private final MappingRedisConverter converter = new MappingRedisConverter(new RedisMappingContext());

    AiUiElementChatSessionCacheEntityTest() {
        converter.afterPropertiesSet();
    }

    @Test
    void roundTripsNestedJsonThroughRedisHashMappingWithoutLosingValues() {
        var json = """
                {"id":"root","type":3,"children":[
                  {"id":"group","type":3,"children":[
                    {"id":"field","type":15,"name":"Größe","value":null}
                  ]},
                  {"id":"empty-group","type":3,"children":[]}
                ],"extension":{"items":[true,42,1.5,null,[],{}],"empty":{},"custom.property/path":"value"}}
                """;
        var entity = new AiUiElementChatSessionCacheEntity().setId("session")
                .setTargetRootType(ElementType.GroupLayout).setCurrentElementJson(json);
        var data = new RedisData();

        converter.write(entity, data);
        var restored = converter.read(AiUiElementChatSessionCacheEntity.class, data);

        assertEquals("session", restored.getId());
        assertEquals(ElementType.GroupLayout, restored.getTargetRootType());
        assertEquals(json, restored.getCurrentElementJson());
        assertArrayEquals(json.getBytes(StandardCharsets.UTF_8), data.getBucket().get("currentElementJson"));
        assertEquals(4 * 60 * 60L, data.getTimeToLive());
        var mapper = JsonMapperTestUtils.createMapper();
        assertEquals(mapper.readTree(json), mapper.readTree(restored.getCurrentElementJson()));
    }

    @Test
    void treatsLegacyMapDataAsMissingJson() {
        var data = new RedisData();
        data.getBucket().put("id", "session".getBytes(StandardCharsets.UTF_8));
        data.getBucket().put("currentElement.[type]", "15".getBytes(StandardCharsets.UTF_8));
        data.getBucket().put("currentElement.[id]", "field".getBytes(StandardCharsets.UTF_8));

        var restored = converter.read(AiUiElementChatSessionCacheEntity.class, data);

        assertEquals("session", restored.getId());
        assertNull(restored.getCurrentElementJson());
    }
}
