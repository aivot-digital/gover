package de.aivot.prosuna.backend.core.jackson;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import tools.jackson.databind.json.JsonMapper;

public class JsonMapperFactoryExtension implements BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
            JsonMapperFactoryExtension.class
    );

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NAMESPACE).put(
                context.getUniqueId(),
                new PreviousMapper(JsonMapperTestUtils.installMapper())
        );
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var previousMapper = context.getStore(NAMESPACE).remove(context.getUniqueId(), PreviousMapper.class);
        JsonMapperTestUtils.restoreMapper(previousMapper.mapper());
    }

    private record PreviousMapper(JsonMapper mapper) {
    }
}
