package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.gover.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.gover.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessInstanceAttachmentSetSelectElement;
import de.aivot.gover.backend.elements.models.elements.form.input.StoragePathSelectorInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.StoragePathSelectorInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.nocode.models.NoCodeExpression;
import de.aivot.gover.backend.nocode.models.NoCodeReference;
import de.aivot.gover.backend.nocode.models.NoCodeStaticValue;
import de.aivot.gover.backend.plugins.core.CorePlugin;
import de.aivot.gover.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodeOutput;
import de.aivot.gover.backend.process.models.ProcessNodePort;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class StoreAttachmentSetActionNodeV1 implements ProcessNodeDefinition<StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig> {
    public static final String NODE_KEY = "store_attachment_set";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_RESULTS = "results";
    private static final String OUTPUT_NAME_STORAGE_PATHS_FROM_ROOT = "storagePathsFromRoot";
    private static final String OUTPUT_NAME_FILE_NAMES = "fileNames";
    private static final String OUTPUT_NAME_COUNT = "count";

    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final StorageService storageService;
    private final StorageProviderRepository storageProviderRepository;

    public StoreAttachmentSetActionNodeV1(TemplateRenderService templateRenderService,
                                          ProcessInstanceAttachmentService processInstanceAttachmentService,
                                          ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                                          StorageService storageService,
                                          StorageProviderRepository storageProviderRepository) {
        this.templateRenderService = templateRenderService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.storageService = storageService;
        this.storageProviderRepository = storageProviderRepository;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Dokument bei Speicheranbieter schreiben";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Speichert die Anhänge eines Anlagensatzes in einem ausgewählten Speicheranbieter.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(StoreAttachmentSetActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für das Speichern eines Anlagensatzes: %s",
                    e.getMessage()
            );
        }

        layout
                .findChild(AttachmentSetStorageConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, ProcessInstanceAttachmentSetSelectElement.class)
                .ifPresent(field -> field
                        .setMinItems(1)
                        .setMaxItems(1));

        layout
                .findChild(AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, StoragePathSelectorInputElement.class)
                .ifPresent(field -> field.setAllowedStorageProviderTypes(List.of(
                        StorageProviderType.Assets,
                        StorageProviderType.External
                )));

        layout
                .findChild(AttachmentSetStorageConfig.FILE_NAME_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> field.setVisibility(ElementVisibilityFunctions
                        .of(NoCodeExpression.of(
                                NoCodeEqualsOperator.OPERATOR_ID,
                                new NoCodeReference(AttachmentSetStorageConfig.CUSTOMIZE_FILE_NAME_FIELD_ID),
                                new NoCodeStaticValue(true)
                        ))
                        .recalculateReferencedIds()));

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Anlagensatz gespeichert",
                        "Der Prozess wird hier fortgesetzt, nachdem die Anhänge gespeichert wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_NAME_RESULTS, "Ablageergebnisse", "Die Ergebnisse je konfiguriertem Anlagensatz."),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PATHS_FROM_ROOT, "Speicherpfade", "Die Pfade der gespeicherten Dateien im Ziel-Speicheranbieter."),
                new ProcessNodeOutput(OUTPUT_NAME_FILE_NAMES, "Dateinamen", "Die Dateinamen der gespeicherten Dateien."),
                new ProcessNodeOutput(OUTPUT_NAME_COUNT, "Anzahl", "Die Anzahl der gespeicherten Dateien.")
        );
    }

    @Nonnull
    @Override
    public Class<StoreAttachmentSetActionNodeConfig> getNodeConfigurationClass() {
        return StoreAttachmentSetActionNodeConfig.class;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull de.aivot.gover.backend.process.entities.ProcessNodeEntity processNodeEntity,
                                                           @Nonnull StoreAttachmentSetActionNodeConfig configuration) {
        var errors = new HashMap<String, List<String>>();

        var attachmentSetConfigs = configuration.attachmentSets == null ? List.<AttachmentSetStorageConfig>of() : configuration.attachmentSets;
        if (attachmentSetConfigs.isEmpty()) {
            addError(errors, StoreAttachmentSetActionNodeConfig.ATTACHMENT_SETS_FIELD_ID, "Es muss mindestens ein Anlagensatz konfiguriert werden.");
        }

        for (var i = 0; i < attachmentSetConfigs.size(); i++) {
            var rowIndex = i + 1;
            var attachmentSetConfig = attachmentSetConfigs.get(i);

            if (resolveSingleAttachmentSetDataKey(attachmentSetConfig.attachmentSetDataKeys) == null) {
                addError(errors, AttachmentSetStorageConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, "Eintrag %d: Es muss genau ein Anlagensatz ausgewählt werden.".formatted(rowIndex));
            }

            var storageProviderId = resolveStorageProviderId(attachmentSetConfig.storagePath);
            if (storageProviderId == null) {
                addError(errors, AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Es muss ein Speicheranbieter ausgewählt werden.".formatted(rowIndex));
            } else {
                var storageProvider = storageProviderRepository.findById(storageProviderId).orElse(null);
                if (storageProvider == null) {
                    addError(errors, AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Der ausgewählte Speicheranbieter wurde nicht gefunden.".formatted(rowIndex));
                } else if (Boolean.TRUE.equals(storageProvider.getReadOnlyStorage())) {
                    addError(errors, AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Der ausgewählte Speicheranbieter ist schreibgeschützt.".formatted(rowIndex));
                } else if (StorageProviderType.Attachments.equals(storageProvider.getType())) {
                    addError(errors, AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Der ausgewählte Speicheranbieter ist ein Speicher für Prozessanlagen und kann nicht als Ziel verwendet werden.".formatted(rowIndex));
                }
            }

            var targetPath = resolveConfiguredPath(attachmentSetConfig.storagePath);
            if (targetPath == null) {
                addError(errors, AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Der Zielpfad muss angegeben werden.".formatted(rowIndex));
            } else {
                var diagnostics = templateRenderService.validateInterpolationSyntax(targetPath);
                if (!diagnostics.isEmpty()) {
                    for (var diagnostic : diagnostics) {
                        addError(
                                errors,
                                AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID,
                                "Eintrag %d, Zeile %d: %s".formatted(rowIndex, diagnostic.lineNumber(), diagnostic.message())
                        );
                    }
                }
            }

            if (Boolean.TRUE.equals(attachmentSetConfig.customizeFileName)
                && StringUtils.isNullOrEmpty(attachmentSetConfig.fileName)) {
                addError(errors, AttachmentSetStorageConfig.FILE_NAME_FIELD_ID, "Eintrag %d: Der Dateiname bei Speicherung muss angegeben werden.".formatted(rowIndex));
            }
        }

        return errors.isEmpty() ? null : errors;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        var attachmentSets = configuration.get(StoreAttachmentSetActionNodeConfig.ATTACHMENT_SETS_FIELD_ID);
        if (attachmentSets instanceof List<?> attachmentSetList) {
            for (var attachmentSet : attachmentSetList) {
                if (!(attachmentSet instanceof Map<?, ?> attachmentSetMap)) {
                    continue;
                }

                var storagePath = attachmentSetMap.get(AttachmentSetStorageConfig.STORAGE_PATH_FIELD_ID);
                if (storagePath instanceof Map<?, ?> storagePathMap) {
                    storagePathMap.remove("storageProviderId");
                }
            }
        }
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<StoreAttachmentSetActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var attachmentSetConfigs = configuration.attachmentSets == null ? List.<AttachmentSetStorageConfig>of() : configuration.attachmentSets;
        if (attachmentSetConfigs.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue("Es muss mindestens ein Anlagensatz konfiguriert werden.");
        }

        var storagePaths = new ArrayList<String>();
        var fileNames = new ArrayList<String>();
        var results = new ArrayList<Map<String, Object>>();
        var createdFolders = new HashSet<String>();

        for (var rowIndex = 0; rowIndex < attachmentSetConfigs.size(); rowIndex++) {
            var attachmentSetConfig = attachmentSetConfigs.get(rowIndex);
            var storageProviderId = resolveRequiredStorageProviderId(attachmentSetConfig.storagePath);
            ensureWritableStorageProvider(storageProviderId);

            var attachmentSetDataKey = resolveSingleAttachmentSetDataKey(attachmentSetConfig.attachmentSetDataKeys);
            if (attachmentSetDataKey == null) {
                throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Es muss genau ein Anlagensatz ausgewählt werden.".formatted(rowIndex + 1));
            }

            var renderedTargetFolderPath = interpolateTargetPath(context, resolveConfiguredPath(attachmentSetConfig.storagePath), rowIndex + 1);
            var normalizedTargetFolderPath = normalizeFolderPath(renderedTargetFolderPath);
            if (normalizedTargetFolderPath == null) {
                throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Der Zielpfad wurde nicht angegeben oder leer interpoliert.".formatted(rowIndex + 1));
            }

            var attachments = resolveProcessAttachmentsBySetDataKey(context, attachmentSetDataKey);
            var setStoragePaths = new ArrayList<String>();
            var setFileNames = new ArrayList<String>();
            var usedCustomFileNames = new LinkedHashSet<String>();

            for (var i = 0; i < attachments.size(); i++) {
                var attachment = attachments.get(i);
                var attachmentIndex = i + 1;
                var targetFolderPath = applyFileIndex(normalizedTargetFolderPath, attachmentIndex);
                var storedFileName = resolveStoredFileName(attachmentSetConfig, attachment.getFileName(), attachmentIndex, usedCustomFileNames, rowIndex + 1);
                var targetPath = appendPathSegment(targetFolderPath, storedFileName);
                ensureParentFolders(storageProviderId, targetPath, createdFolders);

                try (var attachmentContent = storageService.getDocumentContent(
                        attachment.getStorageProviderId(),
                        attachment.getStoragePathFromRoot()
                )) {
                    var storedDocument = storageService.storeDocument(
                            storageProviderId,
                            targetPath,
                            attachmentContent,
                            StorageItemMetadata.empty()
                    );
                    storagePaths.add(storedDocument.getPathFromRoot());
                    fileNames.add(storedDocument.getName());
                    setStoragePaths.add(storedDocument.getPathFromRoot());
                    setFileNames.add(storedDocument.getName());
                } catch (IOException | ResponseException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Der Prozess-Anhang %s konnte nicht im Speicheranbieter %d gespeichert werden: %s",
                            StringUtils.quote(attachment.getFileName()),
                            storageProviderId,
                            e.getMessage()
                    );
                }
            }

            results.add(Map.of(
                    "storageProviderId", storageProviderId,
                    "attachmentSetDataKey", attachmentSetDataKey,
                    "targetFolderPath", normalizedTargetFolderPath,
                    "storagePathsFromRoot", setStoragePaths,
                    "fileNames", setFileNames,
                    "count", setStoragePaths.size()
            ));
        }

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_RESULTS, results);
        metadata.put(OUTPUT_NAME_STORAGE_PATHS_FROM_ROOT, storagePaths);
        metadata.put(OUTPUT_NAME_FILE_NAMES, fileNames);
        metadata.put(OUTPUT_NAME_COUNT, storagePaths.size());

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }

    private void ensureWritableStorageProvider(@Nonnull Integer storageProviderId) throws ProcessNodeExecutionException {
        var storageProvider = storageProviderRepository
                .findById(storageProviderId)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der Speicheranbieter mit der ID %d wurde nicht gefunden.",
                        storageProviderId
                ));

        if (Boolean.TRUE.equals(storageProvider.getReadOnlyStorage())) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Speicheranbieter %s (ID %d) ist schreibgeschützt.",
                    StringUtils.quote(storageProvider.getName()),
                    storageProviderId
            );
        } else if (StorageProviderType.Attachments.equals(storageProvider.getType())) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Speicheranbieter %s (ID %d) ist ein Speicher für Prozessanlagen und kann nicht als Ziel verwendet werden.",
                    StringUtils.quote(storageProvider.getName()),
                    storageProviderId
            );
        }
    }

    @Nullable
    private String resolveSingleAttachmentSetDataKey(@Nullable List<String> attachmentSetDataKeys) {
        if (attachmentSetDataKeys == null) {
            return null;
        }

        var dataKeys = attachmentSetDataKeys
                .stream()
                .map(StringUtils::toNullableTrimmedString)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return dataKeys.size() == 1 ? dataKeys.getFirst() : null;
    }

    @Nonnull
    private List<ProcessInstanceAttachmentEntity> resolveProcessAttachmentsBySetDataKey(@Nonnull ProcessNodeExecutionInitContext<StoreAttachmentSetActionNodeConfig> context,
                                                                                        @Nonnull String attachmentSetDataKey) throws ProcessNodeExecutionException {
        var attachmentSets = processInstanceAttachmentSetService
                .findAllByProcessInstanceIdAndDataKey(context.getThisProcessInstance().getId(), attachmentSetDataKey)
                .stream()
                .sorted(Comparator.comparing(attachmentSet -> attachmentSet.getId()))
                .toList();

        if (attachmentSets.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s wurde in der Prozess-Instanz %d nicht gefunden.",
                    StringUtils.quote(attachmentSetDataKey),
                    context.getThisProcessInstance().getId()
            );
        }

        var attachments = new ArrayList<ProcessInstanceAttachmentEntity>();
        for (var attachmentSet : attachmentSets) {
            attachments.addAll(processInstanceAttachmentService
                    .findAllByAttachmentSetId(attachmentSet.getId())
                    .stream()
                    .sorted(Comparator
                            .comparing(ProcessInstanceAttachmentEntity::getPosition)
                            .thenComparing(attachment -> Objects.toString(attachment.getKey(), "")))
                    .toList());
        }

        if (attachments.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s enthält keine Anhänge.",
                    StringUtils.quote(attachmentSetDataKey)
            );
        }

        return attachments;
    }

    @Nonnull
    private String interpolateTargetPath(@Nonnull ProcessNodeExecutionInitContext<StoreAttachmentSetActionNodeConfig> context,
                                         @Nullable String targetPath,
                                         int rowIndex) throws ProcessNodeExecutionException {
        try {
            var renderedTargetPath = templateRenderService.interpolate(context.getCurrentProcessExecutionData(), targetPath);
            if (renderedTargetPath == null) {
                throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Der Zielpfad wurde nicht angegeben.".formatted(rowIndex));
            }
            return renderedTargetPath;
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(e, "Eintrag %d: Der Zielpfad ist syntaktisch ungültig: %s", rowIndex, e.getMessage());
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Eintrag %d: Der Zielpfad konnte nicht interpoliert werden: %s", rowIndex, e.getMessage());
        }
    }

    @Nonnull
    private static String resolveStoredFileName(@Nonnull AttachmentSetStorageConfig attachmentSetConfig,
                                                @Nonnull String originalFileName,
                                                int attachmentIndex,
                                                @Nonnull Set<String> usedCustomFileNames,
                                                int rowIndex) throws ProcessNodeExecutionException {
        if (!Boolean.TRUE.equals(attachmentSetConfig.customizeFileName)) {
            return originalFileName;
        }

        var configuredFileName = StringUtils.toNullableTrimmedString(attachmentSetConfig.fileName);
        if (configuredFileName == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Der Dateiname bei Speicherung muss angegeben werden.".formatted(rowIndex));
        }

        var configuredBaseFileName = removeExtensionFromConfiguredFileName(configuredFileName)
                .replace("#", Integer.toString(attachmentIndex));
        var resolvedFileName = StringUtils
                .extractExtensionFromFileName(originalFileName)
                .map(extension -> configuredBaseFileName + "." + extension)
                .orElse(configuredBaseFileName);

        resolvedFileName = ensureUniqueFileName(resolvedFileName, usedCustomFileNames);
        validateResolvedFileName(resolvedFileName, rowIndex);
        usedCustomFileNames.add(resolvedFileName);
        return resolvedFileName;
    }

    @Nonnull
    private static String removeExtensionFromConfiguredFileName(@Nonnull String configuredFileName) {
        var lastDotIndex = configuredFileName.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return configuredFileName;
        }

        return configuredFileName.substring(0, lastDotIndex);
    }

    @Nonnull
    private static String ensureUniqueFileName(@Nonnull String requestedFileName,
                                               @Nonnull Set<String> usedFileNames) {
        if (!usedFileNames.contains(requestedFileName)) {
            return requestedFileName;
        }

        for (var suffix = 2; ; suffix++) {
            var candidate = appendNumericSuffix(requestedFileName, suffix);
            if (!usedFileNames.contains(candidate)) {
                return candidate;
            }
        }
    }

    @Nonnull
    private static String appendNumericSuffix(@Nonnull String fileName,
                                              int suffix) {
        var lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return fileName + "-" + suffix;
        }

        return fileName.substring(0, lastDotIndex) +
               "-" +
               suffix +
               fileName.substring(lastDotIndex);
    }

    private static void validateResolvedFileName(@Nonnull String resolvedFileName,
                                                 int rowIndex) throws ProcessNodeExecutionException {
        if (resolvedFileName.length() > 255) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Eintrag %d: Der konfigurierte Dateiname bei Speicherung ist zu lang.",
                    rowIndex
            );
        }

        if (resolvedFileName.contains("/") || resolvedFileName.contains("\\") || resolvedFileName.contains("\r") || resolvedFileName.contains("\n")) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Eintrag %d: Der konfigurierte Dateiname bei Speicherung ist ungültig.",
                    rowIndex
            );
        }
    }

    @Nullable
    private static String normalizeFolderPath(@Nullable String path) {
        var normalizedPath = StringUtils.toNullableTrimmedString(path);
        if (normalizedPath == null) {
            return null;
        }

        normalizedPath = normalizedPath.replace('\\', '/').replaceAll("/+", "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }

        return normalizedPath;
    }

    @Nonnull
    private static String applyFileIndex(@Nonnull String path,
                                         int attachmentIndex) {
        return path.replace("#", Integer.toString(attachmentIndex));
    }

    @Nonnull
    private static String appendPathSegment(@Nonnull String folderPath,
                                            @Nonnull String fileName) {
        if (folderPath.endsWith("/")) {
            return folderPath + fileName;
        }
        return folderPath + "/" + fileName;
    }

    @Nullable
    private static Integer resolveStorageProviderId(@Nullable StoragePathSelectorInputElementValue storagePath) {
        return storagePath == null ? null : storagePath.getStorageProviderId();
    }

    @Nullable
    private static String resolveConfiguredPath(@Nullable StoragePathSelectorInputElementValue storagePath) {
        return storagePath == null ? null : StringUtils.toNullableTrimmedString(storagePath.getPath());
    }

    @Nonnull
    private static Integer resolveRequiredStorageProviderId(@Nullable StoragePathSelectorInputElementValue storagePath) throws ProcessNodeExecutionExceptionMissingValue {
        var storageProviderId = resolveStorageProviderId(storagePath);
        if (storageProviderId == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Der Speicheranbieter muss ausgewählt werden.");
        }
        return storageProviderId;
    }

    private static void addError(@Nonnull Map<String, List<String>> errors,
                                 @Nonnull String fieldId,
                                 @Nonnull String message) {
        errors.computeIfAbsent(fieldId, ignored -> new ArrayList<>()).add(message);
    }

    private void ensureParentFolders(@Nonnull Integer storageProviderId,
                                     @Nonnull String targetPath,
                                     @Nonnull HashSet<String> createdFolders) throws ProcessNodeExecutionException {
        var normalizedPath = targetPath.replace('\\', '/');
        var lastSlashIndex = normalizedPath.lastIndexOf('/');
        if (lastSlashIndex <= 0) {
            return;
        }

        var parentPath = normalizedPath.substring(0, lastSlashIndex);
        var currentPath = new StringBuilder();
        for (var segment : parentPath.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }

            currentPath.append('/').append(segment);
            var folderPath = currentPath + "/";
            if (!createdFolders.add(storageProviderId + ":" + folderPath)) {
                continue;
            }

            try {
                storageService.createFolder(storageProviderId, folderPath);
            } catch (ResponseException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Der Zielordner %s konnte im Speicheranbieter %d nicht erstellt werden: %s",
                        StringUtils.quote(folderPath),
                        storageProviderId,
                        e.getMessage()
                );
            }
        }
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class StoreAttachmentSetActionNodeConfig {
        public static final String ATTACHMENT_SETS_FIELD_ID = "attachment_sets";

        public List<AttachmentSetStorageConfig> attachmentSets;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = StoreAttachmentSetActionNodeConfig.ATTACHMENT_SETS_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensätze"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Konfigurieren Sie alle Anlagensätze, die gespeichert werden sollen."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Anlagensatz #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Anlagensatz hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Anlagensatz entfernen")
    })
    public static class AttachmentSetStorageConfig {
        public static final String ATTACHMENT_SET_DATA_KEYS_FIELD_ID = "attachment_set_data_keys";
        public static final String STORAGE_PATH_FIELD_ID = "storage_path";
        public static final String CUSTOMIZE_FILE_NAME_FIELD_ID = "customize_file_name";
        public static final String FILE_NAME_FIELD_ID = "file_name";

        @InputElementPOJOBinding(id = ATTACHMENT_SET_DATA_KEYS_FIELD_ID, type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensatz"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Anlagensatz der Prozessinstanz, dessen Anhänge gespeichert werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "minItems", intValue = 1),
                @ElementPOJOBindingProperty(key = "maxItems", intValue = 1),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public List<String> attachmentSetDataKeys;

        @InputElementPOJOBinding(id = STORAGE_PATH_FIELD_ID, type = ElementType.StoragePathSelector, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zielpfad"),
                @ElementPOJOBindingProperty(key = "storageProviderSelectHint", strValue = "Speicheranbieter, bei welchem der Anlagensatz gespeichert wird."),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Pfad unter welchem der Anlagensatz gespeichert wird. Verwenden Sie \"#\" zur Angabe der aktuellen Dateinummerierung im Pfad. Diese Eingabe unterstützt \"Smarte Platzhalter\"."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
                @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", falseValue = true)
        })
        public StoragePathSelectorInputElementValue storagePath;

        @InputElementPOJOBinding(id = CUSTOMIZE_FILE_NAME_FIELD_ID, type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateinamen anpassen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wenn aktiviert, wird ein eigener Dateiname bei Speicherung verwendet."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public Boolean customizeFileName;

        @InputElementPOJOBinding(id = FILE_NAME_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateiname bei Speicherung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optional. Wenn gesetzt, wird dieser Wert als Dateiname ohne Endung verwendet. Die Dateiendung kommt immer von der gespeicherten Datei. Werden mehrere Dateien gespeichert, wird ab der zweiten Datei ein Index angehängt, zum Beispiel DATEINAME-2.pdf."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String fileName;
    }
}
