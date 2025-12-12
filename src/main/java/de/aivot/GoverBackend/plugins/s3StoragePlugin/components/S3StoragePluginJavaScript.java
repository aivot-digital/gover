package de.aivot.GoverBackend.plugins.s3StoragePlugin.components;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.springframework.stereotype.Component;

@Component
public class S3StoragePluginJavaScript implements JavascriptFunctionProvider {

    @Override
    public String getObjectName() {
        return "";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[0];
    }
}
