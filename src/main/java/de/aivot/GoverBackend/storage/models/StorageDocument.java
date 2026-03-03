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

    /**
     * Die Größe dieses Dokuments in Bytes. Diese Information ist wichtig, um die Größe des Dokuments zu kennen, z.B. für die Anzeige oder für die Berechnung des verfügbaren
     * Speicherplatzes.
     */
    @Nonnull
    private Long sizeInBytes;

    /**
     * Die Metadaten dieses Dokuments, die zusätzliche Informationen über das Dokument enthalten können. Diese zusätzlichen Informationen werden vom Speicheranbieter definiert und
     * können je nach Implementierung variieren.
     * <p>
     * Beispiele für Metadaten könnten das Erstellungsdatum, das Änderungsdatum, der Autor oder benutzerdefinierte Tags sein. Diese Information ist wichtig, um zusätzliche Details
     * über das Dokument zu erhalten, die für die Verwaltung oder die Anzeige des Dokuments relevant sein könnten.
     */
    @Nonnull
    private StorageItemMetadata metadata;

    public StorageDocument(@Nonnull String pathFromRoot,
                           @Nonnull String name,
                           @Nonnull Long sizeInBytes,
                           @Nonnull StorageItemMetadata metadata) {
        super(pathFromRoot, name, false);

        int lastDotIndex = getName().lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == getName().length() - 1) {
            extension = "dat";
        } else {
            extension = getName().substring(lastDotIndex + 1);
        }
        this.sizeInBytes = sizeInBytes;
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StorageDocument that = (StorageDocument) o;
        return Objects.equals(extension, that.extension) && Objects.equals(sizeInBytes, that.sizeInBytes) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), extension, sizeInBytes, metadata);
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

    @Nonnull
    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    public StorageDocument setSizeInBytes(@Nonnull Long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
        return this;
    }
}
