/**
 * This module contains all general logic to execute javascript code.
 * It is used to execute javascript code in the backend.
 * <br/>
 * <strong>Usage:</strong><br/>
 * Instantiate a new {@link de.aivot.GoverBackend.javascript.services.JavascriptEngine} with the {@link de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService} and call the evaluate method with the javascript code as a string.
 * You can provide Maps as global variables to the service, as well as function providers.
 * The global variables should be used to inject data safely into the vm.
 * The function providers should be used to provide java functions in the javascript context.
 * <br/>
 * <strong>Writing function providers:</strong><br/>
 * A function provider should implement the interface {@link de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider}.
 * All methods in this class, which should be available in the javascript context, should be annotated with the annotation {@link org.graalvm.polyglot.HostAccess.Export} annotation.
 */
package de.aivot.GoverBackend.javascript;