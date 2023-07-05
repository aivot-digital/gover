package de.aivot.GoverBackend.services;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptService {

    public static ScriptEngine getEngine() {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByName("JavaScript");
    }
}
