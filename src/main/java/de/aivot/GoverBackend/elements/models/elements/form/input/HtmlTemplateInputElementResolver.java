package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.services.pdf.MarkdownDialect;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class HtmlTemplateInputElementResolver {
    private static final String SLOT_ATTRIBUTE = "data-slot";
    private static final String SLOT_TYPE_ATTRIBUTE = "data-slot-type";
    private static final String SLOT_TYPE_IMAGE = "image";
    private static final String SLOT_TYPE_TEXT = "text";
    private static final String SLOT_TYPE_RICHTEXT = "richtext";
    private static final Set<String> VOID_ELEMENTS = Set.of(
            "area",
            "base",
            "br",
            "col",
            "embed",
            "hr",
            "img",
            "input",
            "link",
            "meta",
            "param",
            "source",
            "track",
            "wbr"
    );
    private static final MarkdownDialect MARKDOWN_DIALECT = new MarkdownDialect();

    private final AssetService assetService;
    private final StorageService storageService;
    private final TemplateRenderService templateRenderService;

    public HtmlTemplateInputElementResolver(AssetService assetService, StorageService storageService, TemplateRenderService templateRenderService) {
        this.assetService = assetService;
        this.storageService = storageService;
        this.templateRenderService = templateRenderService;
    }

    @Nonnull
    public String resolve(@Nullable HtmlTemplateInputElementValue value, @Nonnull ProcessExecutionData processExecutionData) throws ProcessNodeExecutionException {
        if (value == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Es muss eine Konfiguration für das HTML-Template angegeben sein."
            );
        }

        var htmlTemplateAssetKeyStr = value.getAssetKey();
        if (StringUtils.isNullOrEmpty(htmlTemplateAssetKeyStr)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Es muss eine Datei für die HTML-Vorlage ausgewählt sein."
            );
        }
        UUID htmlTemplateAssetKey;
        try {
            htmlTemplateAssetKey = UUID.fromString(htmlTemplateAssetKeyStr);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der Schlüssel %s für die HTML-Vorlage ist ungültig.",
                    StringUtils.quote(htmlTemplateAssetKeyStr)
            );
        }

        AssetEntity asset;
        try {
            asset = assetService
                    .retrieve(htmlTemplateAssetKey)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionMissingValue(
                            "Die ausgewählte Datei für die HTML-Vorlage existiert nicht."
                    ));
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Das Asset für den angegebenen Schlüssel %s konnte nicht geladen werden.",
                    StringUtils.quote(htmlTemplateAssetKeyStr)
            );
        }

        String templateHtml;
        try (InputStream docStream = storageService.getDocumentContent(asset.getStorageProviderId(), asset.getStoragePathFromRoot())) {
            templateHtml = new String(docStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (ResponseException | IOException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der Inhalt der HTML-Vorlage %s konnte nicht geladen werden.",
                    StringUtils.quote(htmlTemplateAssetKeyStr)
            );
        }

        var renderedTemplates = new HashMap<String, String>();
        if (value.getSlots() != null) {
            for (var slotValueKey : value.getSlots().keySet()) {
                var slotValue = value.getSlots().get(slotValueKey);
                String interpolatedSlotValue;
                try {
                    interpolatedSlotValue = templateRenderService
                            .interpolate(processExecutionData, slotValue);
                } catch (RuntimeException e) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            e,
                            "Der Wert für den Slot %s konnte nicht gerendert werden.",
                            StringUtils.quote(slotValueKey)
                    );
                }
                renderedTemplates.put(slotValueKey, interpolatedSlotValue);
            }
        }

        return applySlotValues(templateHtml, renderedTemplates);
    }

    @Nonnull
    private String applySlotValues(@Nonnull String templateHtml,
                                   @Nonnull Map<String, String> renderedTemplates) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var resolvedHtml = new StringBuilder(templateHtml.length());
        var cursor = 0;

        while (cursor < templateHtml.length()) {
            var tagStart = templateHtml.indexOf('<', cursor);
            if (tagStart < 0) {
                resolvedHtml.append(templateHtml, cursor, templateHtml.length());
                break;
            }

            var startTag = parseStartTag(templateHtml, tagStart);
            if (startTag == null) {
                var tagEnd = findTagEnd(templateHtml, tagStart);
                if (tagEnd < 0) {
                    resolvedHtml.append(templateHtml, cursor, templateHtml.length());
                    break;
                }
                resolvedHtml.append(templateHtml, cursor, tagEnd + 1);
                cursor = tagEnd + 1;
                continue;
            }

            var slotId = startTag.attributeValue(SLOT_ATTRIBUTE);
            if (StringUtils.isNullOrEmpty(slotId)) {
                resolvedHtml.append(templateHtml, cursor, startTag.end() + 1);
                cursor = startTag.end() + 1;
                continue;
            }

            var slotValue = renderedTemplates.get(slotId.trim());
            if (StringUtils.isNullOrEmpty(slotValue)) {
                resolvedHtml.append(templateHtml, cursor, startTag.end() + 1);
                cursor = startTag.end() + 1;
                continue;
            }

            var slotType = startTag.attributeValue(SLOT_TYPE_ATTRIBUTE);
            if (slotType == null) {
                resolvedHtml.append(templateHtml, cursor, startTag.end() + 1);
                cursor = startTag.end() + 1;
                continue;
            }

            slotType = slotType.trim().toLowerCase(Locale.ROOT);

            if (SLOT_TYPE_IMAGE.equals(slotType)) {
                resolvedHtml.append(templateHtml, cursor, startTag.start());
                resolvedHtml.append(setAttribute(templateHtml, startTag, "src", createAssetUrl(slotId, slotValue)));
                cursor = startTag.end() + 1;
                continue;
            }

            if (SLOT_TYPE_TEXT.equals(slotType) || SLOT_TYPE_RICHTEXT.equals(slotType)) {
                var closeTag = findMatchingCloseTag(templateHtml, startTag);
                if (closeTag == null) {
                    resolvedHtml.append(templateHtml, cursor, startTag.end() + 1);
                    cursor = startTag.end() + 1;
                    continue;
                }

                resolvedHtml.append(templateHtml, cursor, startTag.end() + 1);
                if (SLOT_TYPE_TEXT.equals(slotType)) {
                    resolvedHtml.append(HtmlUtils.htmlEscape(slotValue));
                } else {
                    resolvedHtml.append(MARKDOWN_DIALECT.render(slotValue));
                }
                cursor = closeTag.start();
                continue;
            }

            resolvedHtml.append(templateHtml, cursor, startTag.end() + 1);
            cursor = startTag.end() + 1;
        }

        return resolvedHtml.toString();
    }

    @Nonnull
    private String createAssetUrl(@Nonnull String slotId,
                                  @Nonnull String assetKeyValue) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        try {
            return assetService.createUrl(UUID.fromString(assetKeyValue.trim()));
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der Bild-Slot %s enthält einen ungültigen Asset-Schlüssel.",
                    StringUtils.quote(slotId)
            );
        }
    }

    @Nullable
    private StartTag parseStartTag(@Nonnull String html, int start) {
        if (start + 1 >= html.length() || html.charAt(start) != '<') {
            return null;
        }

        var nextChar = html.charAt(start + 1);
        if (nextChar == '/' || nextChar == '!' || nextChar == '?') {
            return null;
        }

        var end = findTagEnd(html, start);
        if (end < 0) {
            return null;
        }

        var tagNameStart = start + 1;
        while (tagNameStart < end && Character.isWhitespace(html.charAt(tagNameStart))) {
            tagNameStart++;
        }

        var tagNameEnd = tagNameStart;
        while (tagNameEnd < end && isTagNameCharacter(html.charAt(tagNameEnd))) {
            tagNameEnd++;
        }

        if (tagNameEnd == tagNameStart) {
            return null;
        }

        var tagName = html
                .substring(tagNameStart, tagNameEnd)
                .toLowerCase(Locale.ROOT);
        var selfClosingSlash = findSelfClosingSlash(html, tagNameEnd, end);
        var selfClosing = selfClosingSlash >= 0 || VOID_ELEMENTS.contains(tagName);
        var attributes = parseAttributes(html, tagNameEnd, end);

        return new StartTag(
                start,
                end,
                tagName,
                selfClosing,
                selfClosingSlash,
                attributes
        );
    }

    private int findTagEnd(@Nonnull String html, int tagStart) {
        Character quote = null;
        for (var i = tagStart + 1; i < html.length(); i++) {
            var ch = html.charAt(i);
            if (quote != null) {
                if (ch == quote) {
                    quote = null;
                }
                continue;
            }

            if (ch == '"' || ch == '\'') {
                quote = ch;
                continue;
            }

            if (ch == '>') {
                return i;
            }
        }
        return -1;
    }

    private int findSelfClosingSlash(@Nonnull String html, int tagNameEnd, int tagEnd) {
        var cursor = tagEnd - 1;
        while (cursor >= tagNameEnd && Character.isWhitespace(html.charAt(cursor))) {
            cursor--;
        }
        return cursor >= tagNameEnd && html.charAt(cursor) == '/' ? cursor : -1;
    }

    @Nonnull
    private Map<String, Attribute> parseAttributes(@Nonnull String html, int start, int end) {
        var attributes = new LinkedHashMap<String, Attribute>();
        var cursor = start;

        while (cursor < end) {
            while (cursor < end && Character.isWhitespace(html.charAt(cursor))) {
                cursor++;
            }

            if (cursor >= end || html.charAt(cursor) == '/') {
                cursor++;
                continue;
            }

            var nameStart = cursor;
            while (
                    cursor < end &&
                            !Character.isWhitespace(html.charAt(cursor)) &&
                            html.charAt(cursor) != '=' &&
                            html.charAt(cursor) != '/'
            ) {
                cursor++;
            }
            var nameEnd = cursor;

            if (nameEnd == nameStart) {
                cursor++;
                continue;
            }

            while (cursor < end && Character.isWhitespace(html.charAt(cursor))) {
                cursor++;
            }

            int valueStart = -1;
            int valueEnd = -1;
            String value = null;

            if (cursor < end && html.charAt(cursor) == '=') {
                cursor++;
                while (cursor < end && Character.isWhitespace(html.charAt(cursor))) {
                    cursor++;
                }

                if (cursor < end && (html.charAt(cursor) == '"' || html.charAt(cursor) == '\'')) {
                    var quote = html.charAt(cursor);
                    cursor++;
                    valueStart = cursor;
                    while (cursor < end && html.charAt(cursor) != quote) {
                        cursor++;
                    }
                    valueEnd = cursor;
                    if (cursor < end) {
                        cursor++;
                    }
                } else {
                    valueStart = cursor;
                    while (cursor < end && !Character.isWhitespace(html.charAt(cursor))) {
                        cursor++;
                    }
                    valueEnd = cursor;
                }

                value = HtmlUtils.htmlUnescape(html.substring(valueStart, valueEnd));
            }

            var name = html.substring(nameStart, nameEnd);
            attributes.put(
                    name.toLowerCase(Locale.ROOT),
                    new Attribute(name, value, nameStart, nameEnd, valueStart, valueEnd)
            );
        }

        return attributes;
    }

    @Nullable
    private CloseTag findMatchingCloseTag(@Nonnull String html,
                                          @Nonnull StartTag startTag) {
        if (startTag.selfClosing()) {
            return null;
        }

        var depth = 1;
        var cursor = startTag.end() + 1;

        while (cursor < html.length()) {
            var nextTagStart = html.indexOf('<', cursor);
            if (nextTagStart < 0) {
                return null;
            }

            if (isCommentStart(html, nextTagStart)) {
                var commentEnd = html.indexOf("-->", nextTagStart + 4);
                cursor = commentEnd < 0 ? html.length() : commentEnd + 3;
                continue;
            }

            var tagEnd = findTagEnd(html, nextTagStart);
            if (tagEnd < 0) {
                return null;
            }

            var closingTagName = parseClosingTagName(html, nextTagStart, tagEnd);
            if (startTag.tagName().equals(closingTagName)) {
                depth--;
                if (depth == 0) {
                    return new CloseTag(nextTagStart, tagEnd);
                }
                cursor = tagEnd + 1;
                continue;
            }

            var nestedStartTag = parseStartTag(html, nextTagStart);
            if (
                    nestedStartTag != null &&
                            startTag.tagName().equals(nestedStartTag.tagName()) &&
                            !nestedStartTag.selfClosing()
            ) {
                depth++;
            }

            cursor = tagEnd + 1;
        }

        return null;
    }

    private boolean isCommentStart(@Nonnull String html, int index) {
        return index + 3 < html.length() && html.startsWith("<!--", index);
    }

    @Nullable
    private String parseClosingTagName(@Nonnull String html, int tagStart, int tagEnd) {
        if (tagStart + 2 >= html.length() || html.charAt(tagStart + 1) != '/') {
            return null;
        }

        var nameStart = tagStart + 2;
        while (nameStart < tagEnd && Character.isWhitespace(html.charAt(nameStart))) {
            nameStart++;
        }

        var nameEnd = nameStart;
        while (nameEnd < tagEnd && isTagNameCharacter(html.charAt(nameEnd))) {
            nameEnd++;
        }

        if (nameEnd == nameStart) {
            return null;
        }

        return html
                .substring(nameStart, nameEnd)
                .toLowerCase(Locale.ROOT);
    }

    private boolean isTagNameCharacter(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':';
    }

    @Nonnull
    private String setAttribute(@Nonnull String html,
                                @Nonnull StartTag startTag,
                                @Nonnull String attributeName,
                                @Nonnull String value) {
        var escapedValue = HtmlUtils.htmlEscape(value);
        var existingAttribute = startTag.attribute(attributeName);
        if (existingAttribute != null) {
            if (existingAttribute.valueStart() >= 0 && existingAttribute.valueEnd() >= 0) {
                return html.substring(startTag.start(), existingAttribute.valueStart()) +
                        escapedValue +
                        html.substring(existingAttribute.valueEnd(), startTag.end() + 1);
            }

            return html.substring(startTag.start(), existingAttribute.nameStart()) +
                    existingAttribute.name() +
                    "=\"" +
                    escapedValue +
                    "\"" +
                    html.substring(existingAttribute.nameEnd(), startTag.end() + 1);
        }

        var insertIndex = startTag.selfClosingSlash() >= 0 ? startTag.selfClosingSlash() : startTag.end();
        var prefix = insertIndex > startTag.start() && Character.isWhitespace(html.charAt(insertIndex - 1)) ? "" : " ";

        return html.substring(startTag.start(), insertIndex) +
                prefix +
                attributeName +
                "=\"" +
                escapedValue +
                "\"" +
                html.substring(insertIndex, startTag.end() + 1);
    }

    private record StartTag(int start,
                            int end,
                            @Nonnull String tagName,
                            boolean selfClosing,
                            int selfClosingSlash,
                            @Nonnull Map<String, Attribute> attributes) {
        @Nullable
        private Attribute attribute(@Nonnull String name) {
            return attributes.get(name.toLowerCase(Locale.ROOT));
        }

        @Nullable
        private String attributeValue(@Nonnull String name) {
            var attribute = attribute(name);
            return attribute == null ? null : attribute.value();
        }
    }

    private record Attribute(@Nonnull String name,
                             @Nullable String value,
                             int nameStart,
                             int nameEnd,
                             int valueStart,
                             int valueEnd) {
    }

    private record CloseTag(int start, int end) {
    }
}
