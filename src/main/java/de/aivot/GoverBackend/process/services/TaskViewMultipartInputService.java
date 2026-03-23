package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class TaskViewMultipartInputService {
    public static final String PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX = "process-instance-attachment:";

    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final AVService avService;

    @Autowired
    public TaskViewMultipartInputService(ProcessInstanceAttachmentService processInstanceAttachmentService,
                                         AVService avService) {
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.avService = avService;
    }

    @Nonnull
    public AuthoredElementValues normalizeInputs(@Nonnull AuthoredElementValues inputs,
                                                 @Nullable MultipartFile[] files,
                                                 @Nullable List<String> fileUris,
                                                 @Nonnull Long processInstanceId,
                                                 @Nullable Long processInstanceTaskId,
                                                 @Nullable String uploadedByUserId) throws ResponseException {
        avService.testMultipartFiles(files);

        var remainingFiles = new LinkedList<MultipartFile>();
        if (files != null) {
            remainingFiles.addAll(Arrays.asList(files));
        }

        var filesByUri = buildFilesByUri(files, fileUris);
        var normalizedInputs = normalizeAuthoredValues(
                inputs,
                remainingFiles,
                filesByUri,
                processInstanceId,
                processInstanceTaskId,
                uploadedByUserId
        );

        if (!remainingFiles.isEmpty()) {
            throw ResponseException.badRequest("Es wurden Dateien übertragen, die keinem Upload-Feld zugeordnet werden konnten.");
        }

        return normalizedInputs;
    }

    @Nonnull
    private AuthoredElementValues normalizeAuthoredValues(@Nonnull AuthoredElementValues inputs,
                                                          @Nonnull List<MultipartFile> remainingFiles,
                                                          @Nonnull Map<String, Deque<MultipartFile>> filesByUri,
                                                          @Nonnull Long processInstanceId,
                                                          @Nullable Long processInstanceTaskId,
                                                          @Nullable String uploadedByUserId) throws ResponseException {
        var normalizedInputs = new AuthoredElementValues();
        for (var entry : inputs.entrySet()) {
            normalizedInputs.put(
                    entry.getKey(),
                    normalizeValue(
                            entry.getValue(),
                            remainingFiles,
                            filesByUri,
                            processInstanceId,
                            processInstanceTaskId,
                            uploadedByUserId
                    )
            );
        }
        return normalizedInputs;
    }

    @Nullable
    private Object normalizeValue(@Nullable Object value,
                                  @Nonnull List<MultipartFile> remainingFiles,
                                  @Nonnull Map<String, Deque<MultipartFile>> filesByUri,
                                  @Nonnull Long processInstanceId,
                                  @Nullable Long processInstanceTaskId,
                                  @Nullable String uploadedByUserId) throws ResponseException {
        if (value instanceof Map<?, ?> rawMap) {
            if (isFileUploadItemMap(rawMap)) {
                return normalizeFileUploadItem(
                        rawMap,
                        remainingFiles,
                        filesByUri,
                        processInstanceId,
                        processInstanceTaskId,
                        uploadedByUserId
                );
            }

            var normalizedMap = new AuthoredElementValues();
            try {
                rawMap.forEach((rawKey, rawChildValue) -> {
                    if (!(rawKey instanceof String key)) {
                        return;
                    }

                    try {
                        normalizedMap.put(
                                key,
                                normalizeValue(
                                        rawChildValue,
                                        remainingFiles,
                                        filesByUri,
                                        processInstanceId,
                                        processInstanceTaskId,
                                        uploadedByUserId
                                )
                        );
                    } catch (ResponseException e) {
                        throw new TaskViewMultipartNormalizationException(e);
                    }
                });
            } catch (TaskViewMultipartNormalizationException e) {
                if (e.getCause() instanceof ResponseException responseException) {
                    throw responseException;
                }
                throw e;
            }
            return normalizedMap;
        }

        if (value instanceof List<?> rawList) {
            var normalizedList = new LinkedList<>();
            for (var rawItem : rawList) {
                normalizedList.add(
                        normalizeValue(
                                rawItem,
                                remainingFiles,
                                filesByUri,
                                processInstanceId,
                                processInstanceTaskId,
                                uploadedByUserId
                        )
                );
            }
            return normalizedList;
        }

        return value;
    }

    @Nonnull
    private Map<String, Object> normalizeFileUploadItem(@Nonnull Map<?, ?> rawItem,
                                                        @Nonnull List<MultipartFile> remainingFiles,
                                                        @Nonnull Map<String, Deque<MultipartFile>> filesByUri,
                                                        @Nonnull Long processInstanceId,
                                                        @Nullable Long processInstanceTaskId,
                                                        @Nullable String uploadedByUserId) throws ResponseException {
        var item = ObjectMapperFactory
                .getInstance()
                .convertValue(rawItem, FileUploadInputElementItem.class);

        if (!requiresUpload(item)) {
            return createFileUploadItemMap(
                    item.getName(),
                    item.getUri(),
                    item.getSize()
            );
        }

        var multipartFile = resolveMultipartFile(item, remainingFiles, filesByUri);
        var fileName = resolveFileName(item, multipartFile);
        validateFileMatchesInput(item, multipartFile, fileName);

        byte[] fileBytes;
        try {
            fileBytes = multipartFile.getBytes();
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Fehler beim Lesen der hochgeladenen Datei.");
        }

        var attachment = ProcessInstanceAttachmentEntity
                .of(fileName, processInstanceId, processInstanceTaskId, fileBytes)
                .setUploadedByUserId(uploadedByUserId);

        var createdAttachment = processInstanceAttachmentService
                .create(attachment);

        return createFileUploadItemMap(
                fileName,
                buildAttachmentUri(createdAttachment.getKey()),
                safeFileSize(multipartFile, fileName)
        );
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
                                          @Nonnull String resolvedFileName) throws ResponseException {
        if (multipartFile.isEmpty()) {
            throw ResponseException.badRequest(
                    "Die Datei „%s“ wurde ohne Inhalt übertragen.",
                    resolvedFileName
            );
        }

        if (item.getName() != null && !item.getName().equals(resolvedFileName)) {
            throw ResponseException.badRequest(
                    "Die hochgeladene Datei „%s“ passt nicht zur erwarteten Datei „%s“.",
                    resolvedFileName,
                    item.getName()
            );
        }

        if (item.getSize() != null && multipartFile.getSize() != item.getSize().longValue()) {
            throw ResponseException.badRequest(
                    "Die hochgeladene Datei „%s“ hat eine unerwartete Größe.",
                    resolvedFileName
            );
        }
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

    private boolean isFileUploadItemMap(@Nonnull Map<?, ?> rawMap) {
        return rawMap.containsKey("name") &&
               rawMap.containsKey("uri") &&
               rawMap.containsKey("size");
    }

    @Nonnull
    private String resolveFileName(@Nonnull FileUploadInputElementItem item,
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

    private int safeFileSize(@Nonnull MultipartFile multipartFile,
                             @Nonnull String fileName) throws ResponseException {
        if (multipartFile.getSize() > Integer.MAX_VALUE) {
            throw ResponseException.badRequest(
                    "Die Datei „%s“ ist zu groß für die Verarbeitung.",
                    fileName
            );
        }

        return (int) multipartFile.getSize();
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

    private static final class TaskViewMultipartNormalizationException extends RuntimeException {
        private TaskViewMultipartNormalizationException(@Nonnull ResponseException cause) {
            super(cause);
        }
    }
}
