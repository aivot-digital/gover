package de.aivot.GoverBackend.javascript.providers;


import de.aivot.GoverBackend.plugin.enums.PluginComponentType;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import jakarta.annotation.Nonnull;

/**
 * Interface for providing functions to the javascript context.
 * The functions will be available in the javascript context under the name returned by {@link #getObjectName}.
 * Your provider should be annotated with {@link org.graalvm.polyglot.HostAccess.Export} to make the functions available in the javascript context.
 * Please make sure, your provider is public.
 */
public interface JavascriptFunctionProvider extends PluginComponent {
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.JavascriptFunctionProvider;
    }

    /**
     * Returns the name of the object in the javascript context.
     *
     * @return the name of the object in the javascript context
     */
    default String getObjectName() {
        return String.format("_%s_v%s", getComponentKey(), getComponentVersion()).toLowerCase();
    }

    /**
     * Returns the type definition of the object in the javascript context.
     * This is used for documentation and intellisense purposes.
     *
     * @return the type definition of the object in the javascript context
     */
    default String getTypeDefinition() {
        var objectName = this.getObjectName();

        var sb = new StringBuilder()
                .append("declare interface I_")
                .append(objectName)
                .append(" {\n");

        for (var methodDef : getMethodTypeDefinitions()) {
            sb.append("    ").append(methodDef).append("\n");
        }

        sb
                .append("}\n")
                .append("declare var ")
                .append(objectName)
                .append(": I_")
                .append(objectName)
                .append(";");

        return sb.toString();
    }

    /**
     * Returns additional method type definitions for the object in the javascript context.
     * This is used for documentation and intellisense purposes.
     * Each method definition should be a valid TypeScript method signature.
     *
     * @return additional method type definitions for the object in the javascript context
     */
    String[] getMethodTypeDefinitions();
}
