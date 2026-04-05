package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Ein StorageFolder repräsentiert einen Ordner im Speichersystem, der sowohl Unterordner als auch Dokumente enthalten kann. Er erweitert die grundlegenden Eigenschaften eines
 * StorageItem um zusätzliche Informationen wie Listen von Unterordnern und Dokumenten sowie eine Flag, die angibt, ob der Ordner rekursiv alle seine Unterordner und Dokumente
 * enthält. Der Pfad eines Ordners muss mit einem {@code "/"} beginnen und enden, z.B. {@code "/folder1/subfolder2/"}.
 */
public class StorageFolder extends StorageItem {
    /**
     * Eine Liste von Unterordnern, die in diesem Ordner enthalten sind. Jeder Unterordner ist ebenfalls ein StorageFolder, was eine hierarchische Struktur ermöglicht.
     */
    @Nonnull
    private List<StorageFolder> subfolders;

    /**
     * Eine Liste von Dokumenten, die in diesem Ordner enthalten sind. Jedes Dokument ist ein StorageDocument, das spezifische Informationen über das Dokument enthält, z.B. den
     * Pfad, den Namen, die Größe und die Metadaten.
     */
    @Nonnull
    private List<StorageDocument> documents;

    /**
     * Eine Flag, die angibt, ob dieser Ordner rekursiv alle seine Unterordner und Dokumente enthält. Wenn {@code true}, bedeutet dies, dass der Ordner nicht nur die direkt in ihm
     * enthaltenen Unterordner und Dokumente enthält, sondern auch alle Elemente in den Unterordnern und deren Unterordnern usw. Diese Information ist wichtig, um zu wissen, ob der
     * Ordner vollständig ist oder ob weitere Elemente in den Unterordnern vorhanden sein könnten, die nicht in der aktuellen Instanz enthalten sind.
     */
    private boolean recursive;

    /**
     * Erstellt einen neuen StorageFolder mit den angegebenen Eigenschaften. Der Pfad muss mit einem {@code "/"} beginnen und enden, z.B. {@code "/folder1/subfolder2/"}.
     *
     * @param pathFromRoot Der Pfad vom Stammverzeichnis zu diesem Ordner, z.B. {@code "/folder1/subfolder2/"}. Dieser Pfad muss mit einem {@code "/"} beginnen und enden, z.B.
     *                     {@code "/folder1/subfolder2/"}.
     * @param name         Der Name dieses Ordners, z.B. {@code "subfolder2"}. Der Name darf keine {@code "/"} enthalten und muss nicht eindeutig sein, da die Identifikation über
     *                     den Pfad erfolgt. Z.B. trägt der Ordner mit dem Pfad {@code "/folder1/subfolder2/"} den Namen {@code "subfolder2"}.
     * @param subfolders   Eine Liste von Unterordnern, die in diesem Ordner enthalten sind. Jeder Unterordner ist ebenfalls ein StorageFolder, was eine hierarchische Struktur
     *                     ermöglicht.
     * @param documents    Eine Liste von Dokumenten, die in diesem Ordner enthalten sind. Jedes Dokument ist ein StorageDocument, das spezifische Informationen über das Dokument
     *                     enthält, z.B. den Pfad,
     * @param recursive    Eine Flag, die angibt, ob dieser Ordner rekursiv alle seine Unterordner und Dokumente enthält. Wenn {@code true}, bedeutet dies, dass der Ordner nicht
     *                     nur die direkt in ihm enthaltenen Unterordner und Dokumente enthält, sondern auch alle Elemente in den Unterordnern und deren Unterordnern usw. Diese
     *                     Information ist wichtig, um zu wissen, ob der Ordner vollständig ist oder ob weitere Elemente in den Unterordnern vorhanden sein könnten, die nicht in
     *                     der aktuellen Instanz enthalten sind.
     */
    public StorageFolder(@Nonnull String pathFromRoot,
                         @Nonnull String name,
                         @Nonnull List<StorageFolder> subfolders,
                         @Nonnull List<StorageDocument> documents,
                         boolean recursive) {
        super(pathFromRoot, name, true);
        this.subfolders = subfolders;
        this.documents = documents;
        this.recursive = recursive;

        if (!(this.getPathFromRoot().startsWith("/") && this.getPathFromRoot().endsWith("/"))) {
            throw new IllegalArgumentException("Folder pathFromRoot must start and end with '/'");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StorageFolder that = (StorageFolder) o;
        return recursive == that.recursive && Objects.equals(subfolders, that.subfolders) && Objects.equals(documents, that.documents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subfolders, documents, recursive);
    }

    public StorageFolder addSubfolder(@Nonnull StorageFolder subfolder) {
        this.subfolders.add(subfolder);
        return this;
    }

    public StorageFolder addDocument(@Nonnull StorageDocument document) {
        this.documents.add(document);
        return this;
    }

    public void apply(@Nonnull Consumer<StorageFolder> folderConsumer) {
        folderConsumer.accept(this);

        for (var subfolder : subfolders) {
            subfolder.apply(folderConsumer);
        }
    }

    public String resolvePath(@Nonnull String relativePath) {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        return this.getPathFromRoot() + relativePath;
    }

    @Nonnull
    public List<StorageFolder> getSubfolders() {
        return subfolders;
    }

    public StorageFolder setSubfolders(@Nonnull List<StorageFolder> subfolders) {
        this.subfolders = subfolders;
        return this;
    }

    @Nonnull
    public List<StorageDocument> getDocuments() {
        return documents;
    }

    public StorageFolder setDocuments(@Nonnull List<StorageDocument> documents) {
        this.documents = documents;
        return this;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public StorageFolder setRecursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }
}
