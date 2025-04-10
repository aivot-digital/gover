package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptService {

    /**
     * @deprecated Use the new {@link JavascriptEngineFactoryService} instead.
     */
    public static ScriptEngine getEngine() {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByName("JavaScript");
    }
}
