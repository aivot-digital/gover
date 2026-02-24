package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Ein StorageDocument repräsentiert ein Dokument im Speichersystem, das spezifische Informationen über das Dokument enthält, z.B. den Pfad, den Namen, die Größe und die Metadaten.
 * Es erweitert die grundlegenden Eigenschaften eines StorageItem um zusätzliche Informationen wie die Dateierweiterung, die Größe in Bytes und die Metadaten. Der Pfad eines
 * Dokuments muss mit einem {@code "/"} beginnen und darf nicht mit einem {@code "/"} enden, z.B. {@code "/folder1/subfolder2/document.pdf"}.
 */
public class StorageDocument extends StorageItem {
    /**
     * Die Dateierweiterung dieses Dokuments, z.B. {@code "pdf"}, {@code "docx"} oder {@code "txt"}. Die Erweiterung wird aus dem Namen des Dokuments extrahiert, indem der Teil
     * nach dem letzten Punkt (.) im Namen genommen wird. Wenn der Name keinen Punkt enthält oder mit einem Punkt endet, wird die Erweiterung als leerer String gesetzt. Diese
     * Information ist wichtig, um den Typ des Dokuments zu bestimmen und die richtige Behandlung zu gewährleisten, z.B. bei der Anzeige oder beim Zugriff auf den Inhalt.
     */
    @Nonnull
    private String extension;

    @Nonnull
    private StorageItemMetadata metadata;

    public StorageDocument(@Nonnull String pathFromRoot,
                           @Nonnull String name,
                           @Nonnull StorageItemMetadata metadata) {
        super(pathFromRoot, name, false);

        int lastDotIndex = getName().lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == getName().length() - 1) {
            extension = "";
        }
        extension = getName().substring(lastDotIndex + 1);
        this.metadata = metadata;
    }

    @Nonnull
    public String getExtension() {
        return extension;
    }

    public StorageDocument setExtension(@Nonnull String extension) {
        this.extension = extension;
        return this;
    }

    @Nonnull
    public StorageItemMetadata getMetadata() {
        return metadata;
    }

    public StorageDocument setMetadata(@Nonnull StorageItemMetadata metadata) {
        this.metadata = metadata;
        return this;
    }
}
