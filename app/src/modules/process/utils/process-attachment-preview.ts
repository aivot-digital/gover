/**
 * Media types that browsers display without executing active content.
 * Attachments can originate from external senders, so other types such as HTML, SVG or XML
 * must never be opened on the application origin. Keep this list aligned with the inline
 * media types of the backend endpoint for process instance attachment files.
 */
const PREVIEWABLE_ATTACHMENT_MEDIA_TYPES: ReadonlySet<string> = new Set([
    'application/pdf',
    'image/png',
    'image/jpeg',
    'image/gif',
    'image/webp',
    'text/plain',
]);

/**
 * Returns the media type to use for previewing an attachment, or null if it must not be previewed.
 */
export function getPreviewableAttachmentMediaType(mediaType: string | null | undefined): string | null {
    const essence = mediaType?.split(';')[0].trim().toLowerCase();

    return essence != null && PREVIEWABLE_ATTACHMENT_MEDIA_TYPES.has(essence) ? essence : null;
}
