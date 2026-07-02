package de.aivot.gover.backend.storage.utils;

import de.aivot.gover.backend.storage.exceptions.StorageException;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class StoragePathUtils {
    private StoragePathUtils() {
    }

    @Nonnull
    public static String normalizeFolderPath(@Nullable String path) throws StorageException {
        var normalizedPath = normalizePath(path);
        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }
        return normalizedPath;
    }

    @Nonnull
    public static String normalizeDocumentPath(@Nullable String path) throws StorageException {
        var normalizedPath = normalizePath(path);
        if ("/".equals(normalizedPath)) {
            throw new StorageException("Ein Dokumentpfad darf nicht auf das Stammverzeichnis zeigen.");
        }
        return trimTrailingSlash(normalizedPath);
    }

    @Nonnull
    public static String trimTrailingSlash(@Nonnull String path) {
        if ("/".equals(path)) {
            return path;
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    @Nonnull
    private static String normalizePath(@Nullable String path) throws StorageException {
        var normalizedPath = StringUtils.isNullOrEmpty(path) ? "/" : path.trim();
        normalizedPath = normalizeSeparators(normalizedPath);
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        normalizedPath = decodePercentEscapes(normalizedPath, path);
        if (normalizedPath.matches(".*%[0-9a-fA-F]{2}.*")) {
            throw invalidPercentEncoding(path);
        }
        normalizedPath = normalizeSeparators(normalizedPath);
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        var segments = normalizedPath.split("/");
        for (var segment : segments) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw new StorageException("Der Pfad %s enthält unzulässige Pfadsegmente.", StringUtils.quote(path));
            }
        }

        return normalizedPath;
    }

    @Nonnull
    private static String normalizeSeparators(@Nonnull String path) {
        return path.replace('\\', '/').replaceAll("/{2,}", "/");
    }

    @Nonnull
    private static String decodePercentEscapes(@Nonnull String path, @Nullable String originalPath) throws StorageException {
        var decodedPath = new StringBuilder(path.length());
        var index = 0;
        while (index < path.length()) {
            var character = path.charAt(index);
            if (character != '%') {
                decodedPath.append(character);
                index++;
                continue;
            }

            var bytes = new ByteArrayOutputStream();
            while (index < path.length() && path.charAt(index) == '%') {
                if (index + 2 >= path.length()) {
                    throw invalidPercentEncoding(originalPath);
                }

                var high = Character.digit(path.charAt(index + 1), 16);
                var low = Character.digit(path.charAt(index + 2), 16);
                if (high < 0 || low < 0) {
                    throw invalidPercentEncoding(originalPath);
                }

                bytes.write((high << 4) + low);
                index += 3;
            }

            decodedPath.append(decodeUtf8(bytes.toByteArray(), originalPath));
        }

        return decodedPath.toString();
    }

    @Nonnull
    private static String decodeUtf8(byte[] bytes, @Nullable String originalPath) throws StorageException {
        var decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new StorageException(e, "Der Pfad %s enthält ungültige Prozentkodierung.", StringUtils.quote(originalPath));
        }
    }

    @Nonnull
    private static StorageException invalidPercentEncoding(@Nullable String path) {
        return new StorageException("Der Pfad %s enthält ungültige Prozentkodierung.", StringUtils.quote(path));
    }
}
