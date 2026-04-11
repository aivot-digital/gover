package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Ein StorageItem repräsentiert ein Element im Speichersystem, das entweder ein Ordner oder ein Dokument sein kann. Es enthält grundlegende Informationen wie den Pfad zum Element,
 * den Namen und die Angabe, ob es sich um einen Ordner handelt. Spezifische Details zu Ordnern und Dokumenten werden in den entsprechenden Unterklassen StorageFolder und
 * StorageDocument definiert.
 */
public abstract class StorageItem {
    /**
     * Der Pfad vom Stammverzeichnis zu diesem Element, z.B. {@code "/folder1/subfolder2/item"}. Dieser Pfad muss mit einem {@code "/"} beginnen und darf nicht mit einem
     * {@code "/"} enden, es sei denn, es handelt sich um einen Ordner.
     */
    @Nonnull
    private String pathFromRoot;

    /**
     * Der Name dieses Elements, z.B. {@code "Antrag.pdf"} oder {@code "subfolder2"}. Der Name darf keine {@code "/"} enthalten und muss nicht eindeutig sein, da die Identifikation
     * über den Pfad erfolgt. Z.B. trägt der Ordner mit dem Pfad {@code "/folder1/subfolder2/"} den Namen {@code "subfolder2"}.
     */
    @Nonnull
    private String name;

    /**
     * Eine Flag, die angibt, ob dieses Element ein Ordner ist. Wenn {@code true}, handelt es sich um einen Ordner, andernfalls um ein Dokument. Diese Information ist wichtig, um
     * die richtige Behandlung des Elements zu gewährleisten, z.B. bei der Anzeige oder beim Zugriff auf den Inhalt.
     */
    private boolean isFolder;

    /**
     * Erstellt ein neues StorageItem mit den angegebenen Eigenschaften. Der Pfad muss mit einem {@code "/"} beginnen und darf nicht mit einem {@code "/"} enden, es sei denn, es
     * handelt sich um einen Ordner. Der Name darf keine {@code "/"} enthalten.
     *
     * @param pathFromRoot Der Pfad vom Stammverzeichnis zu diesem Element, z.B. {@code "/folder1/subfolder2/item"}. Dieser Pfad muss mit einem {@code "/"} beginnen und darf nicht
     *                     mit einem {@code "/"} enden, es sei denn, es handelt sich um einen Ordner.
     * @param name         Der Name dieses Elements, z.B. {@code "Antrag.pdf"} oder {@code "subfolder2"}. Der Name darf keine {@code "/"} enthalten und muss nicht eindeutig sein,
     *                     da die Identifikation über den Pfad erfolgt. Z.B. trägt der Ordner mit dem Pfad {@code "/folder1/subfolder2/"} den Namen {@code "subfolder2"}.
     * @param isFolder     Eine Flag, die angibt, ob dieses Element ein Ordner ist. Wenn {@code true}, handelt es sich um einen Ordner, andernfalls um ein Dokument. Diese
     *                     Information ist wichtig, um die richtige Behandlung des Elements zu gewährleisten, z.B. bei der Anzeige oder beim Zugriff auf den Inhalt.
     */
    public StorageItem(@Nonnull String pathFromRoot,
                       @Nonnull String name,
                       boolean isFolder) {
        this.pathFromRoot = pathFromRoot;
        this.name = name;
        this.isFolder = isFolder;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StorageItem that = (StorageItem) o;
        return isFolder == that.isFolder && Objects.equals(pathFromRoot, that.pathFromRoot) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathFromRoot, name, isFolder);
    }

    @Nonnull
    public String getPathFromRoot() {
        return pathFromRoot;
    }

    public StorageItem setPathFromRoot(@Nonnull String pathFromRoot) {
        this.pathFromRoot = pathFromRoot;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public StorageItem setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public StorageItem setFolder(boolean folder) {
        isFolder = folder;
        return this;
    }
}
