package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.av.services.AVService;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.LayoutElement;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class FileUploadMultipartInputService {
    public static final String PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX = "process-instance-attachment:";

    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final AVService avService;

    @Autowired
    public FileUploadMultipartInputService(ProcessInstanceAttachmentService processInstanceAttachmentService,
                                           ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                                           AVService avService) {
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.avService = avService;
    }

    @Nonnull
    public NormalizationResult normalizeInputs(@Nonnull BaseElement layout,
                                               @Nonnull AuthoredElementValues inputs,
                                               @Nullable MultipartFile[] files,
                                               @Nullable List<String> fileUris,
                                               @Nonnull Long processInstanceId,
                                               @Nullable Long processInstanceTaskId,
                                               @Nullable String uploadedByUserId) throws ResponseException {
        return normalizeInputs(layout, inputs, files, fileUris, processInstanceId, processInstanceTaskId, uploadedByUserId, new LinkedHashMap<>());
    }

    @Nonnull
    private NormalizationResult normalizeInputs(@Nonnull BaseElement layout,
                                                @Nonnull AuthoredElementValues inputs,
                                                @Nullable MultipartFile[] files,
                                                @Nullable List<String> fileUris,
                                                @Nonnull Long processInstanceId,
                                                @Nullable Long processInstanceTaskId,
                                                @Nullable String uploadedByUserId,
                                                @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey) throws ResponseException {
        avService.testMultipartFiles(files);

        var remainingFiles = new LinkedList<MultipartFile>();
        if (files != null) {
            remainingFiles.addAll(Arrays.asList(files));
        }

        var filesByUri = buildFilesByUri(files, fileUris);
        var normalizedInputs = inputs.clone();
        var createdAttachments = new LinkedList<ProcessInstanceAttachmentEntity>();
        var createdFileItems = new LinkedList<FileUploadInputElementItem>();

        normalizeElement(
                layout,
                normalizedInputs,
                remainingFiles,
                filesByUri,
                processInstanceId,
                processInstanceTaskId,
                uploadedByUserId,
                createdAttachments,
                createdFileItems,
                attachmentSetsByDataKey
        );

        if (!remainingFiles.isEmpty()) {
            throw ResponseException.badRequest("Es wurden Dateien übertragen, die keinem Upload-Feld zugeordnet werden konnten.");
        }

        return new NormalizationResult(
                normalizedInputs,
                List.copyOf(createdAttachments),
                List.copyOf(createdFileItems)
        );
    }

    private void normalizeElement(@Nonnull BaseElement element,
                                  @Nonnull AuthoredElementValues values,
                                  @Nonnull List<MultipartFile> remainingFiles,
                                  @Nonnull Map<String, Deque<MultipartFile>> filesByUri,
                                  @Nonnull Long processInstanceId,
                                  @Nullable Long processInstanceTaskId,
                                  @Nullable String uploadedByUserId,
                                  @Nonnull List<ProcessInstanceAttachmentEntity> createdAttachments,
                                  @Nonnull List<FileUploadInputElementItem> createdFileItems,
                                  @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey) throws ResponseException {
        if (element instanceof FileUploadInputElement fileUploadElement) {
            if (!values.containsKey(fileUploadElement.getId())) {
                return;
            }

            values.put(
                    fileUploadElement.getId(),
                    normalizeFileUploadValue(
                            fileUploadElement,
                            values.get(fileUploadElement.getId()),
                            remainingFiles,
                            filesByUri,
                            processInstanceId,
                            processInstanceTaskId,
                            uploadedByUserId,
                            createdAttachments,
                            createdFileItems,
                            attachmentSetsByDataKey
                    )
            );
            return;
        }

        if (element instanceof ReplicatingContainerLayoutElement replicatingContainer) {
            var rawValue = values.get(replicatingContainer.getId());
            if (!(rawValue instanceof List<?> rawRows)) {
                return;
            }

            var normalizedRows = new ArrayList<>(rawRows.size());
            for (var rawRow : rawRows) {
                if (!(rawRow instanceof Map<?, ?> rawRowMap)) {
                    normalizedRows.add(rawRow);
                    continue;
                }

                var rowValues = toAuthoredElementValues(rawRowMap);
                for (var child : replicatingContainer.getChildren()) {
                    normalizeElement(
                            child,
                            rowValues,
                            remainingFiles,
                            filesByUri,
                            processInstanceId,
                            processInstanceTaskId,
                            uploadedByUserId,
                            createdAttachments,
                            createdFileItems,
                            attachmentSetsByDataKey
                    );
                }
                normalizedRows.add(rowValues);
            }

            values.put(replicatingContainer.getId(), normalizedRows);
            return;
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            for (var child : layoutElement.getChildren()) {
                normalizeElement(
                        child,
                        values,
                        remainingFiles,
                        filesByUri,
                        processInstanceId,
                        processInstanceTaskId,
                        uploadedByUserId,
                        createdAttachments,
                        createdFileItems,
                        attachmentSetsByDataKey
                );
            }
        }
    }

    @Nullable
    private Object normalizeFileUploadValue(@Nonnull FileUploadInputElement element,
                                            @Nullable Object rawValue,
                                            @Nonnull List<MultipartFile> remainingFiles,
                                            @Nonnull Map<String, Deque<MultipartFile>> filesByUri,
                                            @Nonnull Long processInstanceId,
                                            @Nullable Long processInstanceTaskId,
                                            @Nullable String uploadedByUserId,
                                            @Nonnull List<ProcessInstanceAttachmentEntity> createdAttachments,
                                            @Nonnull List<FileUploadInputElementItem> createdFileItems,
                                            @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey) throws ResponseException {
        var items = FileUploadInputElement._formatValue(rawValue);
        if (items == null) {
            return rawValue;
        }

        var normalizedItems = new ArrayList<Map<String, Object>>(items.size());
        var usedFileNames = new LinkedHashSet<String>();
        var configuredSubmittedFileName = StringUtils.toNullableTrimmedString(element.getSubmittedFileName());

        for (var item : items) {
            if (!requiresUpload(item) && StringUtils.isNotNullOrEmpty(item.getName())) {
                usedFileNames.add(item.getName());
            }
        }

        for (var itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            var item = items.get(itemIndex);
            if (!requiresUpload(item)) {
                normalizedItems.add(createFileUploadItemMap(
                        item.getName(),
                        item.getUri(),
                        item.getSize()
                ));
                continue;
            }

            var multipartFile = resolveMultipartFile(item, remainingFiles, filesByUri);
            var originalFileName = resolveOriginalFileName(item, multipartFile);
            validateFileMatchesInput(item, multipartFile, originalFileName);

            var finalFileName = configuredSubmittedFileName == null
                    ? originalFileName
                    : resolveConfiguredSubmittedFileName(element, configuredSubmittedFileName, originalFileName, usedFileNames);

            byte[] fileBytes;
            try {
                fileBytes = multipartFile.getBytes();
            } catch (IOException e) {
                throw ResponseException.internalServerError(e, "Fehler beim Lesen der hochgeladenen Datei.");
            }

            var attachment = ProcessInstanceAttachmentEntity
                    .of(finalFileName, itemIndex + 1, processInstanceId, processInstanceTaskId, fileBytes)
                    .setUploadedByUserId(uploadedByUserId);

            var attachmentSet = resolveAttachmentSet(element, processInstanceId, processInstanceTaskId, attachmentSetsByDataKey);
            attachment.setAttachmentSetId(attachmentSet.getId());

            var createdAttachment = processInstanceAttachmentService.create(attachment);
            createdAttachments.add(createdAttachment);
            usedFileNames.add(finalFileName);

            var fileItem = buildAttachmentItem(createdAttachment, multipartFile.getSize());
            createdFileItems.add(fileItem);
            normalizedItems.add(createFileUploadItemMap(fileItem));
        }

        return normalizedItems;
    }

    @Nonnull
    private ProcessInstanceAttachmentSetEntity resolveAttachmentSet(@Nonnull FileUploadInputElement element,
                                                                    @Nonnull Long processInstanceId,
                                                                    @Nullable Long processInstanceTaskId,
                                                                    @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey) throws ResponseException {
        var dataKey = resolveAttachmentSetDataKey(element);
        var attachmentSet = attachmentSetsByDataKey.get(dataKey);
        if (attachmentSet != null) {
            return attachmentSet;
        }

        attachmentSet = processInstanceAttachmentSetService.create(
                new ProcessInstanceAttachmentSetEntity()
                        .setName(resolveAttachmentSetName(element, dataKey))
                        .setDataKey(dataKey)
                        .setProcessInstanceId(processInstanceId)
                        .setProcessInstanceTaskId(processInstanceTaskId)
        );
        attachmentSetsByDataKey.put(dataKey, attachmentSet);
        return attachmentSet;
    }

    @Nonnull
    private String resolveAttachmentSetDataKey(@Nonnull FileUploadInputElement element) throws ResponseException {
        var sourceKey = StringUtils.toNullableTrimmedString(element.getDestinationKey());
        if (sourceKey == null) {
            sourceKey = StringUtils.toNullableTrimmedString(element.getId());
        }
        if (sourceKey == null) {
            throw ResponseException.badRequest("Ein Upload-Feld ohne Datenschlüssel benötigt eine Element-ID.");
        }

        var dataKey = sourceKey.replace('.', '_');
        if (dataKey.length() > 255) {
            throw ResponseException.badRequest("Der Datenschlüssel des Upload-Felds „%s“ ist zu lang.", describeElement(element));
        }

        return dataKey;
    }

    @Nonnull
    private String resolveAttachmentSetName(@Nonnull FileUploadInputElement element,
                                            @Nonnull String dataKey) {
        var label = StringUtils.toNullableTrimmedString(element.getLabel());
        return label == null ? dataKey : StringUtils.truncate(label, 255);
    }

    @Nonnull
    private MultipartFile resolveMultipartFile(@Nonnull FileUploadInputElementItem item,
                                               @Nonnull List<MultipartFile> remainingFiles,
                                               @Nonnull Map<String, Deque<MultipartFile>> filesByUri) throws ResponseException {
        var itemUri = item.getUri();
        if (itemUri != null) {
            var mappedFiles = filesByUri.get(itemUri);
            if (mappedFiles != null && !mappedFiles.isEmpty()) {
                var mappedFile = mappedFiles.pollFirst();
                remainingFiles.remove(mappedFile);
                if (mappedFiles.isEmpty()) {
                    filesByUri.remove(itemUri);
                }
                return mappedFile;
            }
        }

        var iterator = remainingFiles.iterator();
        while (iterator.hasNext()) {
            var candidate = iterator.next();
            if (matchesFile(item, candidate)) {
                iterator.remove();
                return candidate;
            }
        }

        throw ResponseException.badRequest(
                "Für die Datei „%s“ wurden keine Binärdaten übertragen.",
                resolveDisplayName(item)
        );
    }

    private boolean matchesFile(@Nonnull FileUploadInputElementItem item,
                                @Nonnull MultipartFile candidate) {
        var itemName = item.getName();
        var candidateName = candidate.getOriginalFilename();
        if (itemName != null && candidateName != null && !itemName.equals(candidateName)) {
            return false;
        }

        return item.getSize() == null || candidate.getSize() == item.getSize().longValue();
    }

    private void validateFileMatchesInput(@Nonnull FileUploadInputElementItem item,
                                          @Nonnull MultipartFile multipartFile,
                                          @Nonnull String originalFileName) throws ResponseException {
        if (multipartFile.isEmpty()) {
            throw ResponseException.badRequest(
                    "Die Datei „%s“ wurde ohne Inhalt übertragen.",
                    originalFileName
            );
        }

        if (item.getName() != null && !item.getName().equals(originalFileName)) {
            throw ResponseException.badRequest(
                    "Die hochgeladene Datei „%s“ passt nicht zur erwarteten Datei „%s“.",
                    originalFileName,
                    item.getName()
            );
        }

        if (item.getSize() != null && multipartFile.getSize() != item.getSize().longValue()) {
            throw ResponseException.badRequest(
                    "Die hochgeladene Datei „%s“ hat eine unerwartete Größe.",
                    originalFileName
            );
        }
    }

    @Nonnull
    private String resolveConfiguredSubmittedFileName(@Nonnull FileUploadInputElement element,
                                                      @Nonnull String configuredSubmittedFileName,
                                                      @Nonnull String originalFileName,
                                                      @Nonnull Set<String> usedFileNames) throws ResponseException {
        var configuredBaseFileName = removeExtensionFromConfiguredSubmittedFileName(configuredSubmittedFileName);
        var resolvedFileName = StringUtils
                .extractExtensionFromFileName(originalFileName)
                .map(extension -> configuredBaseFileName + "." + extension)
                .orElse(configuredBaseFileName);

        resolvedFileName = ensureUniqueFileName(resolvedFileName, usedFileNames);
        validateResolvedConfiguredFileName(element, resolvedFileName);
        return resolvedFileName;
    }

    @Nonnull
    private String removeExtensionFromConfiguredSubmittedFileName(@Nonnull String configuredSubmittedFileName) {
        var lastDotIndex = configuredSubmittedFileName.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return configuredSubmittedFileName;
        }

        return configuredSubmittedFileName.substring(0, lastDotIndex);
    }

    private void validateResolvedConfiguredFileName(@Nonnull FileUploadInputElement element,
                                                    @Nonnull String resolvedFileName) throws ResponseException {
        if (resolvedFileName.length() > 255) {
            throw ResponseException.badRequest(
                    "Der konfigurierte Dateiname für das Upload-Feld „%s“ ist zu lang.",
                    describeElement(element)
            );
        }

        if (resolvedFileName.contains("/") || resolvedFileName.contains("\\") || resolvedFileName.contains("\r") || resolvedFileName.contains("\n")) {
            throw ResponseException.badRequest(
                    "Der konfigurierte Dateiname für das Upload-Feld „%s“ ist ungültig.",
                    describeElement(element)
            );
        }

        var allowedExtensions = element.getExtensions();
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return;
        }

        var resolvedExtension = StringUtils.extractExtensionFromFileName(resolvedFileName)
                .orElseThrow(() -> ResponseException.badRequest(
                        "Der konfigurierte Dateiname für das Upload-Feld „%s“ benötigt eine erlaubte Dateiendung.",
                        describeElement(element)
                ));

        var extensionAllowed = allowedExtensions
                .stream()
                .anyMatch(allowedExtension -> allowedExtension.equalsIgnoreCase(resolvedExtension));
        if (!extensionAllowed) {
            throw ResponseException.badRequest(
                    "Der konfigurierte Dateiname „%s“ für das Upload-Feld „%s“ hat eine nicht erlaubte Dateiendung.",
                    resolvedFileName,
                    describeElement(element)
            );
        }
    }

    @Nonnull
    private String ensureUniqueFileName(@Nonnull String requestedFileName,
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
    private String appendNumericSuffix(@Nonnull String fileName,
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

    @Nonnull
    private String describeElement(@Nonnull FileUploadInputElement element) {
        return StringUtils.isNotNullOrEmpty(element.getLabel())
                ? Objects.requireNonNull(element.getLabel())
                : element.getId();
    }

    @Nonnull
    private static Map<String, Deque<MultipartFile>> buildFilesByUri(@Nullable MultipartFile[] files,
                                                                     @Nullable List<String> fileUris) {
        if (files == null || fileUris == null || files.length != fileUris.size()) {
            return new HashMap<>();
        }

        var filesByUri = new HashMap<String, Deque<MultipartFile>>();
        for (var i = 0; i < files.length; i++) {
            var uri = fileUris.get(i);
            if (uri == null || uri.isBlank()) {
                continue;
            }

            filesByUri
                    .computeIfAbsent(uri, ignored -> new ArrayDeque<>())
                    .addLast(files[i]);
        }
        return filesByUri;
    }

    private boolean requiresUpload(@Nonnull FileUploadInputElementItem item) {
        var itemUri = item.getUri();
        return itemUri != null && itemUri.startsWith("blob:");
    }

    @Nonnull
    private String resolveOriginalFileName(@Nonnull FileUploadInputElementItem item,
                                           @Nonnull MultipartFile multipartFile) {
        var multipartFileName = multipartFile.getOriginalFilename();
        if (multipartFileName != null && !multipartFileName.isBlank()) {
            return multipartFileName;
        }

        var itemName = item.getName();
        if (itemName != null && !itemName.isBlank()) {
            return itemName;
        }

        return "Unbenannte Datei.dat";
    }

    @Nonnull
    private String resolveDisplayName(@Nonnull FileUploadInputElementItem item) {
        var itemName = item.getName();
        return itemName == null || itemName.isBlank() ? "Unbenannte Datei" : itemName;
    }

    @Nonnull
    public static String buildAttachmentUri(@Nonnull UUID attachmentKey) {
        return PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX + attachmentKey;
    }

    @Nonnull
    public static FileUploadInputElementItem buildAttachmentItem(@Nonnull ProcessInstanceAttachmentEntity attachment,
                                                                long fileSize) throws ResponseException {
        return new FileUploadInputElementItem()
                .setName(attachment.getFileName())
                .setUri(buildAttachmentUri(attachment.getKey()))
                .setSize(safeFileSize(fileSize, attachment.getFileName()));
    }

    private static int safeFileSize(long fileSize,
                                    @Nonnull String fileName) throws ResponseException {
        if (fileSize > Integer.MAX_VALUE) {
            throw ResponseException.badRequest(
                    "Die Datei „%s“ ist zu groß für die Verarbeitung.",
                    fileName
            );
        }

        return (int) fileSize;
    }

    @Nonnull
    private static Map<String, Object> createFileUploadItemMap(@Nullable String name,
                                                               @Nullable String uri,
                                                               @Nullable Integer size) {
        var itemMap = new LinkedHashMap<String, Object>();
        itemMap.put("name", name);
        itemMap.put("uri", uri);
        itemMap.put("size", size);
        return itemMap;
    }

    @Nonnull
    private static Map<String, Object> createFileUploadItemMap(@Nonnull FileUploadInputElementItem item) {
        return createFileUploadItemMap(
                item.getName(),
                item.getUri(),
                item.getSize()
        );
    }

    @Nonnull
    private AuthoredElementValues toAuthoredElementValues(@Nonnull Map<?, ?> rawMap) {
        var authoredValues = new AuthoredElementValues();
        for (var entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                authoredValues.put(key, entry.getValue());
            }
        }
        return authoredValues;
    }

    public record NormalizationResult(
            @Nonnull AuthoredElementValues inputs,
            @Nonnull List<ProcessInstanceAttachmentEntity> createdAttachments,
            @Nonnull List<FileUploadInputElementItem> createdFileItems
    ) {
        public NormalizationResult(@Nonnull AuthoredElementValues inputs,
                                   @Nonnull List<ProcessInstanceAttachmentEntity> createdAttachments) {
            this(inputs, createdAttachments, List.of());
        }
    }
}
