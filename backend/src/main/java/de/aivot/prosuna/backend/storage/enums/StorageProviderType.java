package de.aivot.prosuna.backend.storage.enums;

public enum StorageProviderType {
    Assets("Dateien und Medien"),
    Attachments("Vorgangsanlagen"),
    External("Externe Dateiablage"),
    ;

    private final String label;

    StorageProviderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
