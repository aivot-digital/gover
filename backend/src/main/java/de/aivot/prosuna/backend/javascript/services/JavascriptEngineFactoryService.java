package de.aivot.prosuna.backend.javascript.services;

import de.aivot.prosuna.backend.javascript.enums.JavascriptExecutionContext;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for creating instances of the {@link JavascriptEngine} class.
 * This is necessary because the {@link JavascriptEngine} class cannot be a Spring bean and therefore cannot be injected into other classes.
 * The {@link JavascriptEngine} class is not a Spring bean because it is a resource that needs to be closed after use and bears some context information.
 */
@Service
public class JavascriptEngineFactoryService {
    private final List<JavascriptFunctionProvider> functionProviders;

    @Autowired
    public JavascriptEngineFactoryService(List<JavascriptFunctionProvider> functionProviders) {
        this.functionProviders = functionProviders;
    }

    /**
     * Creates a new instance of the {@link JavascriptEngine} class.
     *
     * @return A new instance of the {@link JavascriptEngine} class.
     */
    public JavascriptEngine getEngine() {
        return getEngine(JavascriptExecutionContext.Default);
    }

    /**
     * Creates a new instance of the {@link JavascriptEngine} class that only provides the function providers available in the given context.
     *
     * @param context The context in which the code is executed.
     * @return A new instance of the {@link JavascriptEngine} class.
     */
    public JavascriptEngine getEngine(JavascriptExecutionContext context) {
        return new JavascriptEngine(functionProviders, context);
    }
}
