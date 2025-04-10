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
}
