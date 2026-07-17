// Extract a file type map from the AppConfig which is provided by the backend via the app-config.js endpoint
const typeMap: Record<string, string> = AppConfig
    .knownFileExtensions
    .reduce((map, ext) => {
        map[ext.mime] = ext.name;
        return map;
    }, {} as Record<string, string>);

const typeGroupMap: Record<string, string> = {
    image: 'Bilder',
    audio: 'Audiodateien',
    video: 'Videodateien',
    text: 'Textdateien',
};

function normalizeContentType(contentType: string): string {
    return contentType.trim().toLowerCase();
}

/**
 * Returns a human-readable file type label for a given content type.
 * @param contentType - The MIME type of the file.
 * @returns A string describing the file type.
 */
export function getFileTypeLabel(contentType: string): string {
    // Return file type label or a fallback
    return typeMap[normalizeContentType(contentType)] || 'Unbekannte Datei';
}

/**
 * Returns a human-readable label for a content type filter.
 * Supports exact MIME types such as "image/png" and broad filters such as "image/" or "image".
 */
export function getFileTypeFilterLabel(contentTypeFilter: string): string {
    const normalized = normalizeContentType(contentTypeFilter)
        .replace(/\/\*$/, '')
        .replace(/\/$/, '');

    if (normalized.length === 0) {
        return 'Alle Dateitypen';
    }

    if (!normalized.includes('/')) {
        return typeGroupMap[normalized] ?? normalized;
    }

    return typeMap[normalized] ?? normalized;
}

export function getFileTypeFilterSummary(contentTypeFilters: string[]): string {
    const labels = contentTypeFilters
        .map(getFileTypeFilterLabel)
        .filter((label) => label.length > 0);

    if (labels.length === 0) {
        return 'Alle Dateitypen';
    }

    return labels.join(', ');
}
