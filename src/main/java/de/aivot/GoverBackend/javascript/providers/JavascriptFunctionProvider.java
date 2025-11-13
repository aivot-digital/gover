package de.aivot.GoverBackend.javascript.providers;

import de.aivot.GoverBackend.serviceprovider.providers.ServiceProvider;

/**
 * Interface for providing functions to the javascript context.
 * The functions will be available in the javascript context under the name returned by {@link #getObjectName}.
 * Your provider should be annotated with {@link org.graalvm.polyglot.HostAccess.Export} to make the functions available in the javascript context.
 * Please make sure, your provider is public.
 * Refer to {@link ServiceProvider} for more information.
 */
public interface JavascriptFunctionProvider extends ServiceProvider {
    /**
     * Returns the name of the object in the javascript context.
     * This name is derived from the package name of the service provider interface.
     * Because dots are not allowed in javascript object names, the dots are replaced by underscores.
     *
     * @return the name of the object in the javascript context
     */
    default String getObjectName() {
        return getPackageName().replaceAll("\\.", "_");
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
