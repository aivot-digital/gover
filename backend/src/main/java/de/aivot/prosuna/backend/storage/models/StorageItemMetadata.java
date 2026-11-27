package de.aivot.prosuna.backend.storage.models;

import java.util.HashMap;

public class StorageItemMetadata extends HashMap<String, Object> {
    public static StorageItemMetadata empty() {
        return new StorageItemMetadata();
    }
}
