package de.aivot.gover.backend.storage.enums;

public enum StorageProviderType {
    Assets("Dokumente und Medien"),
    Attachments("Prozessanlagen"),
    External("Externe Dokumentenablage"),
    ;

    private final String label;

    StorageProviderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
