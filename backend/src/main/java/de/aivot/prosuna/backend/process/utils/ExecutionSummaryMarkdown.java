package de.aivot.prosuna.backend.process.utils;

import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Formatting for persisted execution snapshots. Node-specific wording belongs to the definition. */
public final class ExecutionSummaryMarkdown {
    public static final String RUNTIME_KEY = "executionSummary";
    public static final String SENT_AT = "sentAt";
    public static final String DELIVERY_CHANNEL = "deliveryChannel";
    public static final String PREVIOUS_ASSIGNED_USER_ID = "previousAssignedUserId";
    public static final String PREVIOUS_ASSIGNED_USER_NAME = "previousAssignedUserName";
    public static final String ASSIGNED_USER_NAME = "assignedUserName";
    public static final String DOCUMENTS = "documents";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm:ss z", Locale.GERMAN);
    private final ProcessNodeExecutionSummaryContext<?> context;

    public ExecutionSummaryMarkdown(@Nonnull ProcessNodeExecutionSummaryContext<?> context) {
        this.context = context;
    }

    @Nullable
    public Object data(@Nonnull String key) {
        return map(context.thisTask().getNodeData()).get(key);
    }

    @Nullable
    public Object metadata(@Nonnull String key) {
        return map(map(context.thisTask().getRuntimeData()).get(RUNTIME_KEY)).get(key);
    }

    @Nonnull
    public String value(@Nonnull String key) {
        return text(data(key));
    }

    /** Includes the preposition so missing historical timestamps can be omitted without broken wording. */
    @Nonnull
    public String at(@Nullable Object timestamp) {
        var formatted = timestamp(timestamp);
        return formatted.isEmpty() ? "" : " am " + formatted;
    }

    @Nonnull
    public String completedAt() {
        return at(context.thisTask().getFinished());
    }

    @Nonnull
    public String eventAt(@Nonnull String key) {
        return timestamp(data(key)).isEmpty() ? completedAt() : at(data(key));
    }

    @Nonnull
    public String actor() {
        var user = context.triggeringUser();
        return user != null ? user(user.getId(), user.getFullName()) : user(data("processedByUserId"), null);
    }

    @Nonnull
    public String identity(@Nullable Object identityId) {
        if (identityId == null) {
            return "die empfangende Person";
        }
        var identities = context.thisProcessInstance().getIdentities();
        var identity = identities == null ? null : identities.get(identityId.toString());
        return "die Identität „" + text(identity != null && identity.title() != null && !identity.title().isBlank()
                ? identity.title() + " (" + identityId + ")" : identityId) + "“";
    }

    @Nonnull
    public String delivery() {
        var channel = text(metadata(DELIVERY_CHANNEL));
        return channel.isEmpty() ? "" : " via " + channel;
    }

    @Nonnull
    public String taskLink(@Nonnull String label) {
        return "[" + text(label) + "](/staff/tasks/" + segment(context.thisProcessInstance().getId())
                + "/" + segment(context.thisTask().getId()) + ")";
    }

    @Nonnull
    public static String user(@Nullable Object id, @Nullable Object name) {
        if (id == null) {
            return "das System";
        }
        var label = name == null || name.toString().isBlank() ? id : name;
        return "[" + text(label) + "](/staff/users/" + segment(id) + ")";
    }

    @Nonnull
    public static String document(@Nullable Object key, @Nullable Object name) {
        var label = name == null ? "Dokument ansehen" : name;
        return key == null ? text(label) : "[" + text(label) + "](/api/process-instance-attachments/"
                + segment(key) + "/file/?download=false)";
    }

    @Nonnull
    public static String text(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        // Escape plain values, including HTML and table delimiters; authored Markdown uses section() instead.
        return value.toString().replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("([\\\\`*_{}\\[\\]()<>#+.!|~&-])", "\\\\$1");
    }

    @Nonnull
    public static String section(@Nonnull String title, @Nullable Object markdown) {
        return markdown == null || markdown.toString().isBlank() ? "" : "\n\n**" + title + "**\n\n" + markdown;
    }

    @Nonnull
    public static String detail(@Nonnull String title, @Nullable Object value) {
        return section(title, text(value));
    }

    @Nonnull
    public static String timestamp(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        try {
            var instant = value instanceof Instant time ? time : OffsetDateTime.parse(value.toString()).toInstant();
            return TIMESTAMP.format(instant.atZone(ApplicationTimeZone.getZoneId()));
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    /** URLs in a summary must not reveal embedded credentials, query tokens or fragments. */
    @Nonnull
    public static String safeUrl(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        try {
            var uri = URI.create(value.toString());
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return "";
            }
            return text(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null));
        } catch (Exception ignored) {
            return "";
        }
    }

    @Nonnull
    public static Map<?, ?> map(@Nullable Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    @Nonnull
    public static List<?> list(@Nullable Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    @Nonnull
    public static Map<String, Object> withMetadata(@Nullable Map<String, Object> runtimeData,
                                                   @Nonnull Map<String, ?> updates) {
        var result = new LinkedHashMap<String, Object>(runtimeData == null ? Map.of() : runtimeData);
        var metadata = new LinkedHashMap<String, Object>();
        map(result.get(RUNTIME_KEY)).forEach((key, value) -> metadata.put(key.toString(), value));
        metadata.putAll(updates);
        result.put(RUNTIME_KEY, metadata);
        return result;
    }

    /** Keep only summary receipts when a result intentionally replaces other runtime state. */
    @Nonnull
    public static Map<String, Object> preserveMetadata(@Nullable Map<String, Object> previous,
                                                       @Nullable Map<String, Object> next) {
        var result = new LinkedHashMap<String, Object>(next == null ? Map.of() : next);
        var metadata = new LinkedHashMap<String, Object>();
        map(map(previous).get(RUNTIME_KEY)).forEach((key, value) -> metadata.put(key.toString(), value));
        map(result.get(RUNTIME_KEY)).forEach((key, value) -> metadata.put(key.toString(), value));
        if (!metadata.isEmpty()) {
            result.put(RUNTIME_KEY, metadata);
        }
        return result;
    }

    @Nonnull
    private static String segment(@Nullable Object value) {
        return URLEncoder.encode(value == null ? "" : value.toString(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
