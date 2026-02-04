package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StorageFolder extends StorageItem {
    /**
     * Optional metadata associated with this folder.
     */
    @Nullable
    private Map<String, Object> metadata;

    /**
     * A list of subfolders contained within this folder.
     */
    @Nonnull
    private List<StorageFolder> subfolders;

    /**
     * A list of documents contained within this folder.
     */
    @Nonnull
    private List<StorageDocument> documents;

    /**
     * Weather the folder contains its subfolders and documents recursively.
     */
    private boolean recursive;

    public StorageFolder(@Nonnull String pathFromRoot,
                         @Nonnull String name,
                         @Nullable Map<String, Object> metadata,
                         @Nonnull List<StorageFolder> subfolders,
                         @Nonnull List<StorageDocument> documents,
                         boolean recursive) {
        super(pathFromRoot, name, true);
        this.metadata = metadata;
        this.subfolders = subfolders;
        this.documents = documents;
        this.recursive = recursive;

        if (!(this.getPathFromRoot().startsWith("/") && this.getPathFromRoot().endsWith("/"))) {
            throw new IllegalArgumentException("Folder pathFromRoot must start and end with '/'");
        }
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

    @Nullable
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public StorageFolder setMetadata(@Nullable Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
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
