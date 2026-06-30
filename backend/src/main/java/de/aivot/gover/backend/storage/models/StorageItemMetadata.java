package de.aivot.gover.backend.storage.models;

import java.util.HashMap;

public class StorageItemMetadata extends HashMap<String, Object> {
    public static StorageItemMetadata empty() {
        return new StorageItemMetadata();
    }
}
