package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.ComputedElementSubState;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.utils.StringUtils;
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

    /**
     * Stores the uploaded files of the upload fields whose authored value the derivation of the inputs takes, and
     * replaces their transient items with references to the stored attachments.
     * <p>
     * Hidden upload fields and upload fields whose value the derivation does not take, such as disabled or technical
     * fields in customer input, store nothing. Their transient items are removed and their files discarded, because
     * the frontend sends them with legitimate submissions as well.
     *
     * @param elementStates the element states of the derivation of {@code inputs}
     */
    @Nonnull
    public NormalizationResult normalizeInputs(@Nonnull BaseElement layout,
                                               @Nonnull AuthoredElementValues inputs,
                                               @Nonnull ComputedElementStates elementStates,
                                               @Nullable MultipartFile[] files,
                                               @Nullable List<String> fileUris,
                                               @Nonnull Long processInstanceId,
                                               @Nullable Long processInstanceTaskId,
                                               @Nullable String uploadedByUserId) throws ResponseException {
        return normalizeInputs(layout, inputs, elementStates, files, fileUris, processInstanceId, processInstanceTaskId, uploadedByUserId, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    @Nonnull
    private NormalizationResult normalizeInputs(@Nonnull BaseElement layout,
                                                @Nonnull AuthoredElementValues inputs,
                                                @Nonnull ComputedElementStates elementStates,
                                                @Nullable MultipartFile[] files,
                                                @Nullable List<String> fileUris,
                                                @Nonnull Long processInstanceId,
                                                @Nullable Long processInstanceTaskId,
                                                @Nullable String uploadedByUserId,
                                                @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey,
                                                @Nonnull Map<String, Integer> nextAttachmentPositionsByDataKey) throws ResponseException {
        avService.testMultipartFiles(files);

        var remainingFiles = new LinkedList<MultipartFile>();
        if (files != null) {
            remainingFiles.addAll(Arrays.asList(files));
        }

        var filesByUri = buildFilesByUri(files, fileUris);
        var normalizedInputs = inputs.clone();
        var createdAttachments = new LinkedList<ProcessInstanceAttachmentEntity>();
        var createdFileItems = new LinkedList<FileUploadInputElementItem>();
        var skippedFileItems = new LinkedList<FileUploadInputElementItem>();

        normalizeElement(
                layout,
                normalizedInputs,
                elementStates,
                remainingFiles,
                filesByUri,
                processInstanceId,
                processInstanceTaskId,
                uploadedByUserId,
                createdAttachments,
                createdFileItems,
                skippedFileItems,
                attachmentSetsByDataKey,
                nextAttachmentPositionsByDataKey,
                null,
                false
        );

        // Discard the files of skipped upload fields only after the stored fields took theirs, so a skipped field
        // cannot claim a file that a stored field matches by name and size.
        discardSkippedFiles(skippedFileItems, remainingFiles, filesByUri);

        if (!remainingFiles.isEmpty()) {
            throw ResponseException.badRequest("Es wurden Dateien übertragen, die keinem Anlagen-Feld zugeordnet werden konnten.");
        }

        return new NormalizationResult(
                normalizedInputs,
                List.copyOf(createdAttachments),
                List.copyOf(createdFileItems)
        );
    }

    private void normalizeElement(@Nonnull BaseElement element,
                                  @Nonnull AuthoredElementValues values,
                                  @Nonnull ComputedElementStates elementStates,
                                  @Nonnull List<MultipartFile> remainingFiles,
                                  @Nonnull Map<String, Deque<MultipartFile>> filesByUri,
                                  @Nonnull Long processInstanceId,
                                  @Nullable Long processInstanceTaskId,
                                  @Nullable String uploadedByUserId,
                                  @Nonnull List<ProcessInstanceAttachmentEntity> createdAttachments,
                                  @Nonnull List<FileUploadInputElementItem> createdFileItems,
                                  @Nonnull List<FileUploadInputElementItem> skippedFileItems,
                                  @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey,
                                  @Nonnull Map<String, Integer> nextAttachmentPositionsByDataKey,
                                  @Nullable String attachmentGroup,
                                  boolean hasMissingAttachmentGroupPart) throws ResponseException {
        if (element instanceof FileUploadInputElement fileUploadElement) {
            if (!values.containsKey(fileUploadElement.getId())) {
                return;
            }

            var authoredValue = values.get(fileUploadElement.getId());
            if (!(authoredValue instanceof LiteralAuthoredInputValue literal)) {
                return;
            }

            var elementState = elementStates.get(fileUploadElement.getId());
            if (elementState == null || !elementState.isAuthoredValueAdopted()) {
                skipFileUploadValue(fileUploadElement, values, literal.value(), skippedFileItems);
                return;
            }

            values.putLiteral(
                    fileUploadElement.getId(),
                    normalizeFileUploadValue(
                            fileUploadElement,
                            literal.value(),
                            remainingFiles,
                            filesByUri,
                            processInstanceId,
                            processInstanceTaskId,
                            uploadedByUserId,
                            createdAttachments,
                            createdFileItems,
                            attachmentSetsByDataKey,
                            nextAttachmentPositionsByDataKey,
                            attachmentGroup,
                            hasMissingAttachmentGroupPart
                    )
            );
            return;
        }

        if (element instanceof ReplicatingContainerLayoutElement replicatingContainer) {
            var rawValue = values.getLiteral(replicatingContainer.getId());
            if (!(rawValue instanceof List<?> rawRows)) {
                return;
            }

            // Rows of a container whose value the derivation does not take have no states, so their upload fields are
            // skipped as well.
            var containerState = elementStates.get(replicatingContainer.getId());
            var rowStates = containerState != null && containerState.isAuthoredValueAdopted()
                    ? containerState.getSubStates()
                    : null;

            var normalizedRows = new ArrayList<>(rawRows.size());
            for (var rowIndex = 0; rowIndex < rawRows.size(); rowIndex++) {
                var rawRow = rawRows.get(rowIndex);
                var rowValue = resolveReplicatingContainerRowValue(rawRow);
                if (rowValue == null) {
                    normalizedRows.add(rawRow);
                    continue;
                }

                var rowValues = rowValue.getValues() != null ? rowValue.getValues() : new AuthoredElementValues();
                var rowId = StringUtils.toNullableTrimmedString(rowValue.getId());
                var childAttachmentGroup = appendAttachmentGroup(attachmentGroup, rowId);
                var childHasMissingAttachmentGroupPart = hasMissingAttachmentGroupPart || rowId == null;
                var rowElementStates = resolveRowElementStates(rowStates, rowIndex, rowValue.getId());
                for (var child : replicatingContainer.getChildren()) {
                    normalizeElement(
                            child,
                            rowValues,
                            rowElementStates,
                            remainingFiles,
                            filesByUri,
                            processInstanceId,
                            processInstanceTaskId,
                            uploadedByUserId,
                            createdAttachments,
                            createdFileItems,
                            skippedFileItems,
                            attachmentSetsByDataKey,
                            nextAttachmentPositionsByDataKey,
                            childAttachmentGroup,
                            childHasMissingAttachmentGroupPart
                    );
                }
                normalizedRows.add(rowValue.setValues(rowValues));
            }

            values.putLiteral(replicatingContainer.getId(), normalizedRows);
            return;
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            for (var child : layoutElement.getChildren()) {
                normalizeElement(
                        child,
                        values,
                        elementStates,
                        remainingFiles,
                        filesByUri,
                        processInstanceId,
                        processInstanceTaskId,
                        uploadedByUserId,
                        createdAttachments,
                        createdFileItems,
                        skippedFileItems,
                        attachmentSetsByDataKey,
                        nextAttachmentPositionsByDataKey,
                        attachmentGroup,
                        hasMissingAttachmentGroupPart
                );
            }
        }
    }

    /**
     * Removes the transient items of an upload field whose value the derivation does not take, so the inputs keep no
     * reference to a file that is not stored. Items of attachments stored before stay unchanged.
     */
    private void skipFileUploadValue(@Nonnull FileUploadInputElement element,
                                     @Nonnull AuthoredElementValues values,
                                     @Nullable Object rawValue,
                                     @Nonnull List<FileUploadInputElementItem> skippedFileItems) {
        var items = FileUploadInputElement._formatValue(rawValue);
        if (items == null || items.stream().noneMatch(this::requiresUpload)) {
            return;
        }

        var keptItems = new ArrayList<Map<String, Object>>(items.size());
        for (var item : items) {
            if (requiresUpload(item)) {
                skippedFileItems.add(item);
            } else {
                keptItems.add(createFileUploadItemMap(item));
            }
        }

        if (keptItems.isEmpty()) {
            values.remove(element.getId());
        } else {
            values.putLiteral(element.getId(), keptItems);
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
                                            @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey,
                                            @Nonnull Map<String, Integer> nextAttachmentPositionsByDataKey,
                                            @Nullable String attachmentGroup,
                                            boolean hasMissingAttachmentGroupPart) throws ResponseException {
        var items = FileUploadInputElement._formatValue(rawValue);
        if (items == null) {
            return rawValue;
        }

        var attachmentSetDataKey = resolveAttachmentSetDataKey(element);
        var normalizedItems = new ArrayList<Map<String, Object>>(items.size());
        var configuredSubmittedFileName = StringUtils.toNullableTrimmedString(element.getSubmittedFileName());

        for (var itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            var item = items.get(itemIndex);
            var itemPosition = nextAttachmentPositionsByDataKey.getOrDefault(attachmentSetDataKey, 1);
            nextAttachmentPositionsByDataKey.put(attachmentSetDataKey, itemPosition + 1);

            if (!requiresUpload(item)) {
                normalizedItems.add(createFileUploadItemMap(
                        item.getName(),
                        item.getOriginalFileName(),
                        item.getUri(),
                        item.getSize()
                ));
                continue;
            }

            if (hasMissingAttachmentGroupPart) {
                throw ResponseException.badRequest(
                        "Für das Anlagen-Feld „%s“ fehlt eine ID des Listeneintrags.",
                        describeUploadElement(element)
                );
            }

            var multipartFile = resolveMultipartFile(item, remainingFiles, filesByUri);
            var originalFileName = resolveOriginalFileName(item, multipartFile);
            validateFileMatchesInput(item, multipartFile, originalFileName);

            if (configuredSubmittedFileName == null) {
                throw ResponseException.badRequest(
                        "Für das Anlagen-Feld „%s“ muss ein Dateiname bei Einreichung konfiguriert sein.",
                        describeUploadElement(element)
                );
            }

            var finalFileName = resolveConfiguredSubmittedFileName(
                    element,
                    configuredSubmittedFileName,
                    originalFileName,
                    resolveFileNameIndices(element, itemIndex)
            );

            byte[] fileBytes;
            try {
                fileBytes = multipartFile.getBytes();
            } catch (IOException e) {
                throw ResponseException.internalServerError(e, "Fehler beim Lesen der hochgeladenen Datei.");
            }

            var attachment = ProcessInstanceAttachmentEntity
                    .of(finalFileName, originalFileName, attachmentGroup, itemPosition, processInstanceId, processInstanceTaskId, fileBytes)
                    .setUploadedByUserId(uploadedByUserId);

            var attachmentSet = resolveAttachmentSet(element, attachmentSetDataKey, processInstanceId, processInstanceTaskId, attachmentSetsByDataKey);
            attachment.setAttachmentSetId(attachmentSet.getId());

            var createdAttachment = processInstanceAttachmentService.create(attachment);
            createdAttachments.add(createdAttachment);

            var fileItem = buildAttachmentItem(createdAttachment, multipartFile.getSize());
            createdFileItems.add(fileItem);
            normalizedItems.add(createFileUploadItemMap(fileItem));
        }

        return normalizedItems;
    }

    @Nonnull
    private ProcessInstanceAttachmentSetEntity resolveAttachmentSet(@Nonnull FileUploadInputElement element,
                                                                    @Nonnull String dataKey,
                                                                    @Nonnull Long processInstanceId,
                                                                    @Nullable Long processInstanceTaskId,
                                                                    @Nonnull Map<String, ProcessInstanceAttachmentSetEntity> attachmentSetsByDataKey) throws ResponseException {
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
            throw ResponseException.badRequest("Ein Anlagen-Feld ohne Datenschlüssel benötigt eine Element-ID.");
        }

        var dataKey = sourceKey.replace('.', '_');
        if (dataKey.length() > 255) {
            throw ResponseException.badRequest("Der Datenschlüssel des Anlagen-Felds „%s“ ist zu lang.", describeUploadElement(element));
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

    /**
     * Discards the files of skipped transient items. A missing file is no error, because the frontend need not send
     * the files of fields the derivation does not take.
     */
    private void discardSkippedFiles(@Nonnull List<FileUploadInputElementItem> skippedFileItems,
                                     @Nonnull List<MultipartFile> remainingFiles,
                                     @Nonnull Map<String, Deque<MultipartFile>> filesByUri) {
        for (var item : skippedFileItems) {
            var mappedFiles = item.getUri() != null ? filesByUri.get(item.getUri()) : null;
            if (mappedFiles != null && !mappedFiles.isEmpty()) {
                remainingFiles.remove(mappedFiles.pollFirst());
                continue;
            }

            remainingFiles
                    .stream()
                    .filter(candidate -> matchesFile(item, candidate))
                    .findFirst()
                    .ifPresent(remainingFiles::remove);
        }
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
                                                      @Nonnull List<Integer> fileNameIndices) throws ResponseException {
        var configuredBaseFileName = applyFileNameIndices(
                removeExtensionFromConfiguredSubmittedFileName(configuredSubmittedFileName),
                fileNameIndices
        );
        var resolvedFileName = StringUtils
                .extractExtensionFromFileName(originalFileName)
                .map(extension -> configuredBaseFileName + "." + extension)
                .orElse(configuredBaseFileName);

        validateResolvedConfiguredFileName(element, resolvedFileName);
        return resolvedFileName;
    }

    @Nullable
    private String appendAttachmentGroup(@Nullable String attachmentGroup,
                                         @Nullable String itemId) {
        if (itemId == null) {
            return attachmentGroup;
        }

        return attachmentGroup == null ? itemId : attachmentGroup + "/" + itemId;
    }

    @Nonnull
    private List<Integer> resolveFileNameIndices(@Nonnull FileUploadInputElement element,
                                                 int itemIndex) {
        if (!Boolean.TRUE.equals(element.getIsMultifile())) {
            return List.of();
        }

        return List.of(itemIndex + 1);
    }

    @Nonnull
    private String applyFileNameIndices(@Nonnull String configuredBaseFileName,
                                        @Nonnull List<Integer> fileNameIndices) {
        if (fileNameIndices.isEmpty()) {
            return configuredBaseFileName;
        }

        var index = String.join("-", fileNameIndices.stream().map(String::valueOf).toList());
        return configuredBaseFileName.contains("#")
                ? configuredBaseFileName.replace("#", index)
                : configuredBaseFileName + "-" + index;
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
                    "Der konfigurierte Dateiname für das Anlagen-Feld „%s“ ist zu lang.",
                    describeUploadElement(element)
            );
        }

        if (resolvedFileName.contains("/") || resolvedFileName.contains("\\") || resolvedFileName.contains("\r") || resolvedFileName.contains("\n")) {
            throw ResponseException.badRequest(
                    "Der konfigurierte Dateiname für das Anlagen-Feld „%s“ ist ungültig.",
                    describeUploadElement(element)
            );
        }

        var allowedExtensions = element.getExtensions();
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return;
        }

        var resolvedExtension = StringUtils.extractExtensionFromFileName(resolvedFileName)
                .orElseThrow(() -> ResponseException.badRequest(
                        "Der konfigurierte Dateiname für das Anlagen-Feld „%s“ benötigt eine erlaubte Dateiendung.",
                        describeUploadElement(element)
                ));

        var extensionAllowed = allowedExtensions
                .stream()
                .anyMatch(allowedExtension -> allowedExtension.equalsIgnoreCase(resolvedExtension));
        if (!extensionAllowed) {
            throw ResponseException.badRequest(
                    "Der konfigurierte Dateiname „%s“ für das Anlagen-Feld „%s“ hat eine nicht erlaubte Dateiendung.",
                    resolvedFileName,
                    describeUploadElement(element)
            );
        }
    }

    @Nonnull
    public static String describeUploadElement(@Nonnull FileUploadInputElement uploadElement) {
        var label = StringUtils.toNullableTrimmedString(uploadElement.getLabel());
        if (label == null) {
            return Objects.toString(uploadElement.getId(), "Unbenanntes Anlagen-Feld");
        }
        return label;
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
                .setOriginalFileName(attachment.getOriginalFileName())
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
                                                               @Nullable String originalFileName,
                                                               @Nullable String uri,
                                                               @Nullable Integer size) {
        var itemMap = new LinkedHashMap<String, Object>();
        itemMap.put("name", name);
        itemMap.put("originalFileName", originalFileName);
        itemMap.put("uri", uri);
        itemMap.put("size", size);
        return itemMap;
    }

    @Nonnull
    private static Map<String, Object> createFileUploadItemMap(@Nonnull FileUploadInputElementItem item) {
        return createFileUploadItemMap(
                item.getName(),
                item.getOriginalFileName(),
                item.getUri(),
                item.getSize()
        );
    }

    @Nullable
    private ReplicatingContainerLayoutElementValue resolveReplicatingContainerRowValue(@Nullable Object rawRow) {
        if (!(rawRow instanceof ReplicatingContainerLayoutElementValue) && !(rawRow instanceof Map<?, ?>)) {
            return null;
        }

        var rows = ReplicatingContainerLayoutElement._formatValue(List.of(rawRow));
        return rows != null && !rows.isEmpty() ? rows.getFirst() : null;
    }

    /**
     * Finds the element states of a row by its position. The derivation keeps one state per row of the authored value
     * in the same order. Matching by position instead of by ID keeps rows apart that share an ID, and the ID check
     * skips a row whose state belongs to another row.
     */
    @Nonnull
    private static ComputedElementStates resolveRowElementStates(@Nullable List<ComputedElementSubState> rowStates,
                                                                 int rowIndex,
                                                                 @Nullable String rowId) {
        if (rowStates == null || rowIndex >= rowStates.size()) {
            return new ComputedElementStates();
        }

        var rowState = rowStates.get(rowIndex);
        return Objects.equals(rowState.getId(), rowId) ? rowState.getStates() : new ComputedElementStates();
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
