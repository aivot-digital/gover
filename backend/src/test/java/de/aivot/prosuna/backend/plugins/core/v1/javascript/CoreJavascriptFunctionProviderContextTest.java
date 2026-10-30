package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.javascript.enums.JavascriptExecutionContext;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class CoreJavascriptFunctionProviderContextTest {
    static Stream<Arguments> providers() {
        return Stream.of(
                Arguments.of(CodeListJavascriptV1.class, true),
                Arguments.of(DateJavascriptV1.class, true),
                Arguments.of(NumberJavascriptV1.class, true),
                Arguments.of(OrgJavascriptV1.class, true),
                Arguments.of(UserJavascriptV1.class, true),
                Arguments.of(XRepositoryCodelistJavascriptV1.class, true),
                Arguments.of(AssetJavascriptV1.class, false),
                Arguments.of(DataObjectJavascriptV1.class, false),
                Arguments.of(HttpJavascriptV1.class, false),
                Arguments.of(ProcessAttachmentSetJavascriptV1.class, false),
                Arguments.of(SecretJavascriptV1.class, false)
        );
    }

    @ParameterizedTest
    @MethodSource("providers")
    void isAvailableIn_BlocksSecretsRequestsFilesAttachmentsAndDataObjectsInAssetTemplates(Class<? extends JavascriptFunctionProvider> providerClass,
                                                                                boolean availableInAssetTemplates) {
        var provider = mock(providerClass, CALLS_REAL_METHODS);

        assertTrue(provider.isAvailableIn(JavascriptExecutionContext.Default));
        assertEquals(availableInAssetTemplates, provider.isAvailableIn(JavascriptExecutionContext.AssetTemplate));
    }
}
