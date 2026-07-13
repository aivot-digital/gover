package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.gover.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.gover.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessInstanceAttachmentSetSelectElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.core.CorePlugin;
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
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class StoreAttachmentSetActionNodeV1 implements ProcessNodeDefinition<StoreAttachmentSetActionNodeV1.StoreAttachmentSetActionNodeConfig> {
    public static final String NODE_KEY = "store_attachment_set";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_STORAGE_PROVIDER_ID = "storageProviderId";
    private static final String OUTPUT_NAME_ATTACHMENT_SET_DATA_KEY = "attachmentSetDataKey";
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
                .findChild(StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> field.setOptions(storageProviderRepository
                        .findAll()
                        .stream()
                        .filter(provider -> !Boolean.TRUE.equals(provider.getReadOnlyStorage()))
                        .sorted(Comparator.comparing(StorageProviderEntity::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(provider -> SelectInputElementOption.of(
                                provider.getId().toString(),
                                provider.getName(),
                                provider.getType().name()
                        ))
                        .toList()));

        layout
                .findChild(StoreAttachmentSetActionNodeConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, ProcessInstanceAttachmentSetSelectElement.class)
                .ifPresent(field -> field
                        .setMinItems(1)
                        .setMaxItems(1));

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
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PROVIDER_ID, "Speicheranbieter-ID", "Die ID des Ziel-Speicheranbieters."),
                new ProcessNodeOutput(OUTPUT_NAME_ATTACHMENT_SET_DATA_KEY, "Anlagensatz", "Der Datenschlüssel des gespeicherten Anlagensatzes."),
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

        try {
            var storageProviderId = parseStorageProviderId(configuration.storageProviderId);
            var storageProvider = storageProviderRepository.findById(storageProviderId).orElse(null);
            if (storageProvider == null) {
                errors.put(StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID, List.of("Der ausgewählte Speicheranbieter wurde nicht gefunden."));
            } else if (Boolean.TRUE.equals(storageProvider.getReadOnlyStorage())) {
                errors.put(StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID, List.of("Der ausgewählte Speicheranbieter ist schreibgeschützt."));
            }
        } catch (ProcessNodeExecutionException e) {
            errors.put(StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID, List.of(e.getMessage()));
        }

        if (resolveSingleAttachmentSetDataKey(configuration.attachmentSetDataKeys) == null) {
            errors.put(StoreAttachmentSetActionNodeConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, List.of("Es muss genau ein Anlagensatz ausgewählt werden."));
        }

        if (StringUtils.isNullOrEmpty(configuration.targetPath)) {
            errors.put(StoreAttachmentSetActionNodeConfig.TARGET_PATH_FIELD_ID, List.of("Der Zielpfad muss angegeben werden."));
        } else {
            var diagnostics = templateRenderService.validateInterpolationSyntax(configuration.targetPath);
            if (!diagnostics.isEmpty()) {
                errors.put(
                        StoreAttachmentSetActionNodeConfig.TARGET_PATH_FIELD_ID,
                        diagnostics.stream()
                                .map(diagnostic -> "Zeile %d: %s".formatted(diagnostic.lineNumber(), diagnostic.message()))
                                .toList()
                );
            }
        }

        return errors.isEmpty() ? null : errors;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(StoreAttachmentSetActionNodeConfig.STORAGE_PROVIDER_ID_FIELD_ID);
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<StoreAttachmentSetActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var storageProviderId = parseStorageProviderId(configuration.storageProviderId);
        ensureWritableStorageProvider(storageProviderId);

        var attachmentSetDataKey = resolveSingleAttachmentSetDataKey(configuration.attachmentSetDataKeys);
        if (attachmentSetDataKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Es muss genau ein Anlagensatz ausgewählt werden.");
        }

        var renderedTargetPath = interpolateTargetPath(context, configuration.targetPath);
        if (StringUtils.isNullOrEmpty(renderedTargetPath)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Der Zielpfad wurde nicht angegeben oder leer interpoliert.");
        }

        var attachments = resolveProcessAttachmentsBySetDataKey(context, attachmentSetDataKey)
                .stream()
                .sorted(Comparator
                        .comparing(ProcessInstanceAttachmentEntity::getFileName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(attachment -> Objects.toString(attachment.getKey(), "")))
                .toList();

        var storagePaths = new ArrayList<String>();
        var fileNames = new ArrayList<String>();
        var createdFolders = new HashSet<String>();

        for (var i = 0; i < attachments.size(); i++) {
            var attachment = attachments.get(i);
            var targetPath = resolveTargetPath(renderedTargetPath.trim(), attachment.getFileName(), i + 1);
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

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_STORAGE_PROVIDER_ID, storageProviderId);
        metadata.put(OUTPUT_NAME_ATTACHMENT_SET_DATA_KEY, attachmentSetDataKey);
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
        }
    }

    @Nonnull
    private Integer parseStorageProviderId(@Nullable String rawStorageProviderId) throws ProcessNodeExecutionException {
        var storageProviderId = StringUtils.toNullableTrimmedString(rawStorageProviderId);
        if (storageProviderId == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Der Speicheranbieter muss ausgewählt werden.");
        }

        try {
            return Integer.parseInt(storageProviderId);
        } catch (NumberFormatException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Speicheranbieter-ID %s ist ungültig.",
                    StringUtils.quote(storageProviderId)
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
                .findAllByProcessInstanceIdAndDataKey(context.getThisProcessInstance().getId(), attachmentSetDataKey);

        if (attachmentSets.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s wurde in der Prozess-Instanz %d nicht gefunden.",
                    StringUtils.quote(attachmentSetDataKey),
                    context.getThisProcessInstance().getId()
            );
        }

        var attachments = new ArrayList<ProcessInstanceAttachmentEntity>();
        for (var attachmentSet : attachmentSets) {
            attachments.addAll(processInstanceAttachmentService.findAllByAttachmentSetId(attachmentSet.getId()));
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
                                         @Nullable String targetPath) throws ProcessNodeExecutionException {
        try {
            var renderedTargetPath = templateRenderService.interpolate(context.getCurrentProcessExecutionData(), targetPath);
            if (renderedTargetPath == null) {
                throw new ProcessNodeExecutionExceptionMissingValue("Der Zielpfad wurde nicht angegeben.");
            }
            return renderedTargetPath;
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(e, "Der Zielpfad ist syntaktisch ungültig: %s", e.getMessage());
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Der Zielpfad konnte nicht interpoliert werden: %s", e.getMessage());
        }
    }

    @Nonnull
    private static String resolveTargetPath(@Nonnull String renderedPath,
                                            @Nonnull String originalFileName,
                                            int attachmentIndex) {
        var basePath = removeConfiguredExtension(renderedPath);
        if (basePath.contains("#")) {
            basePath = basePath.replace("#", Integer.toString(attachmentIndex));
        } else if (attachmentIndex > 1) {
            basePath = appendNumericSuffix(basePath, attachmentIndex);
        }

        return basePath + extractOriginalExtension(originalFileName);
    }

    @Nonnull
    private static String removeConfiguredExtension(@Nonnull String path) {
        var lastSlashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        var lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex <= lastSlashIndex + 1 || lastDotIndex == path.length() - 1) {
            return path;
        }

        var configuredExtension = path.substring(lastDotIndex + 1);
        if (configuredExtension.contains("#")) {
            return path;
        }

        return path.substring(0, lastDotIndex);
    }

    @Nonnull
    private static String appendNumericSuffix(@Nonnull String path,
                                              int suffix) {
        return path + "-" + suffix;
    }

    @Nonnull
    private static String extractOriginalExtension(@Nonnull String originalFileName) {
        var lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == originalFileName.length() - 1) {
            return "";
        }

        return originalFileName.substring(lastDotIndex);
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
            if (!createdFolders.add(folderPath)) {
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
        public static final String STORAGE_PROVIDER_ID_FIELD_ID = "storage_provider_id";
        public static final String ATTACHMENT_SET_DATA_KEYS_FIELD_ID = "attachment_set_data_keys";
        public static final String TARGET_PATH_FIELD_ID = "target_path";

        @InputElementPOJOBinding(id = STORAGE_PROVIDER_ID_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Speicheranbieter"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Speicheranbieter, in dem die Anhänge gespeichert werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String storageProviderId;

        @InputElementPOJOBinding(id = ATTACHMENT_SET_DATA_KEYS_FIELD_ID, type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensatz"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Anlagensatz der Prozessinstanz, dessen Anhänge gespeichert werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "minItems", intValue = 1),
                @ElementPOJOBindingProperty(key = "maxItems", intValue = 1)
        })
        public List<String> attachmentSetDataKeys;

        @InputElementPOJOBinding(id = TARGET_PATH_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zielpfad"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Pfad im Ziel-Speicheranbieter. Vorlagen-Tags sind erlaubt. Optional kann # als 1-basierter Dateiindex verwendet werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String targetPath;
    }
}
