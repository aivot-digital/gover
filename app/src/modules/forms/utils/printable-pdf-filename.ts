const FallbackFilenameBase = 'Formulareingang';
const FilenameBaseMaxLength = 120;

function sanitizeFilenameBase(value: string): string {
    return value
        .replace(/\.pdf$/i, '')
        .replace(/[<>:"/\\|?*]/g, '')
        .split('')
        .filter((char) => char.charCodeAt(0) >= 32)
        .join('')
        .replace(/\s+/g, ' ')
        .trim()
        .replace(/\.+$/g, '')
        .slice(0, FilenameBaseMaxLength)
        .trim();
}

export function resolvePrintablePdfFilename(...candidates: Array<string | null | undefined>): string {
    for (const candidate of [...candidates, FallbackFilenameBase]) {
        if (candidate == null) {
            continue;
        }

        const filenameBase = sanitizeFilenameBase(candidate);
        if (filenameBase.length > 0) {
            return `${filenameBase}.pdf`;
        }
    }

    return `${FallbackFilenameBase}.pdf`;
}
