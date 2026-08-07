package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Objects;

@Component
public class ProcessAttachmentSetJavascriptV1 implements JavascriptFunctionProvider {
    private static final String ATTACHMENT_TYPE_DEFINITION = "{ key: string; filename: string; originalFilename: string; group: string | null; position: number; attachmentSetId: number; processInstanceId: number; processInstanceTaskId: number | null; storageProviderId: number; storagePathFromRoot: string; }";

    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;

    public ProcessAttachmentSetJavascriptV1(ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                                            @Lazy ProcessInstanceAttachmentService processInstanceAttachmentService,
                                            ProcessInstanceRepository processInstanceRepository,
                                            ProcessInstanceTaskRepository processInstanceTaskRepository) {
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "attachments";
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
    public String getName() {
        return "Anlagensätze";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Anlagensätze von Vorgängen.";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "create(dataKey: string, name: string, processInstanceId: number, processInstanceTaskId: number): number;",
                "addAttachmentBase64(attachmentSetId: number, fileName: string, base64Content: string): " + ATTACHMENT_TYPE_DEFINITION + ";",
                "addAttachmentString(attachmentSetId: number, fileName: string, content: string): " + ATTACHMENT_TYPE_DEFINITION + ";"
        };
    }

    @HostAccess.Export
    public int create(@Nullable String dataKey,
                      @Nullable String name,
                      @Nullable Number processInstanceId,
                      @Nullable Number processInstanceTaskId) throws ResponseException {
        var normalizedDataKey = requireText(dataKey, "dataKey");
        var normalizedName = requireText(name, "name");
        var normalizedProcessInstanceId = requireLong(processInstanceId, "processInstanceId");
        var normalizedProcessInstanceTaskId = requireLong(processInstanceTaskId, "processInstanceTaskId");

        ensureAttachmentSetCanBeChanged(normalizedProcessInstanceId, normalizedProcessInstanceTaskId);

        var created = processInstanceAttachmentSetService.create(
                new ProcessInstanceAttachmentSetEntity()
                        .setDataKey(normalizedDataKey)
                        .setName(normalizedName)
                        .setProcessInstanceId(normalizedProcessInstanceId)
                        .setProcessInstanceTaskId(normalizedProcessInstanceTaskId)
        );

        return created.getId();
    }

    @HostAccess.Export
    public ProxyObject addAttachmentBase64(@Nullable Number attachmentSetId,
                                           @Nullable String fileName,
                                           @Nullable String base64Content) throws ResponseException {
        var normalizedAttachmentSetId = requireInt(attachmentSetId, "attachmentSetId");
        var normalizedFileName = requireText(fileName, "fileName");
        var content = decodeBase64Content(base64Content);

        return addAttachment(normalizedAttachmentSetId, normalizedFileName, content);
    }

    @HostAccess.Export
    public ProxyObject addAttachmentString(@Nullable Number attachmentSetId,
                                           @Nullable String fileName,
                                           @Nullable String content) throws ResponseException {
        var normalizedAttachmentSetId = requireInt(attachmentSetId, "attachmentSetId");
        var normalizedFileName = requireText(fileName, "fileName");
        var fileBytes = encodeStringContent(content);

        return addAttachment(normalizedAttachmentSetId, normalizedFileName, fileBytes);
    }

    @Nonnull
    private ProxyObject addAttachment(@Nonnull Integer normalizedAttachmentSetId,
                                      @Nonnull String normalizedFileName,
                                      @Nonnull byte[] content) throws ResponseException {
        var attachmentSet = processInstanceAttachmentSetService
                .retrieve(normalizedAttachmentSetId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment set not found: " + normalizedAttachmentSetId));

        ensureAttachmentSetCanBeChanged(attachmentSet.getProcessInstanceId(), attachmentSet.getProcessInstanceTaskId());

        var position = processInstanceAttachmentService
                .findAllByAttachmentSetId(normalizedAttachmentSetId)
                .stream()
                .map(ProcessInstanceAttachmentEntity::getPosition)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        var attachment = ProcessInstanceAttachmentEntity
                .of(
                        normalizedFileName,
                        position,
                        attachmentSet.getProcessInstanceId(),
                        attachmentSet.getProcessInstanceTaskId(),
                        content
                )
                .setAttachmentSetId(normalizedAttachmentSetId);

        return attachmentToProxyObject(processInstanceAttachmentService.create(attachment));
    }

    private void ensureAttachmentSetCanBeChanged(@Nonnull Long processInstanceId,
                                                 @Nullable Long processInstanceTaskId) {
        var processInstance = processInstanceRepository
                .findById(processInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Process instance not found: " + processInstanceId));

        if (processInstance.getStatus() != ProcessInstanceStatus.Running) {
            throw new IllegalStateException("Cannot change attachment sets for process instance that is not running: " + processInstanceId);
        }

        if (processInstanceTaskId == null) {
            return;
        }

        var task = processInstanceTaskRepository
                .findById(processInstanceTaskId)
                .orElseThrow(() -> new IllegalArgumentException("Process instance task not found: " + processInstanceTaskId));

        if (!Objects.equals(task.getProcessInstanceId(), processInstanceId)) {
            throw new IllegalArgumentException("Process instance task does not belong to process instance: " + processInstanceTaskId);
        }

        if (task.getStatus() != ProcessTaskStatus.Running) {
            throw new IllegalStateException("Cannot change attachment sets for process instance task that is not running: " + processInstanceTaskId);
        }
    }

    @Nonnull
    private static String requireText(@Nullable String value,
                                      @Nonnull String fieldName) {
        var normalizedValue = StringUtils.toNullableTrimmedString(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException(fieldName + " must not be empty.");
        }
        if (normalizedValue.length() > 255) {
            throw new IllegalArgumentException(fieldName + " must not be longer than 255 characters.");
        }
        return normalizedValue;
    }

    @Nonnull
    private static Long requireLong(@Nullable Number value,
                                    @Nonnull String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null.");
        }
        return value.longValue();
    }

    @Nonnull
    private static Integer requireInt(@Nullable Number value,
                                      @Nonnull String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null.");
        }
        return value.intValue();
    }

    @Nonnull
    private static byte[] decodeBase64Content(@Nullable String base64Content) {
        var normalizedBase64Content = StringUtils.toNullableTrimmedString(base64Content);
        if (normalizedBase64Content == null) {
            throw new IllegalArgumentException("base64Content must not be empty.");
        }

        byte[] content;
        try {
            content = Base64
                    .getMimeDecoder()
                    .decode(normalizedBase64Content);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("base64Content must be valid Base64.", e);
        }

        if (content.length == 0) {
            throw new IllegalArgumentException("base64Content must not decode to an empty file.");
        }
        return content;
    }

    @Nonnull
    private static byte[] encodeStringContent(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content must not be empty.");
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Nonnull
    private static ProxyObject attachmentToProxyObject(@Nonnull ProcessInstanceAttachmentEntity attachment) {
        var data = new LinkedHashMap<String, Object>();
        data.put("key", attachment.getKey().toString());
        data.put("filename", attachment.getFileName());
        data.put("originalFilename", attachment.getOriginalFileName());
        data.put("group", attachment.getGroup());
        data.put("position", attachment.getPosition());
        data.put("attachmentSetId", attachment.getAttachmentSetId());
        data.put("processInstanceId", attachment.getProcessInstanceId());
        data.put("processInstanceTaskId", attachment.getProcessInstanceTaskId());
        data.put("storageProviderId", attachment.getStorageProviderId());
        data.put("storagePathFromRoot", attachment.getStoragePathFromRoot());
        return JavascriptEngine.mapToProxyObject(data);
    }
}
