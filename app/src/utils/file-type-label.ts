/**
 * Returns a human-readable file type label for a given content type.
 * @param contentType - The MIME type of the file.
 * @returns A string describing the file type.
 */
export function getFileTypeLabel(contentType: string): string {
    const typeMap: Record<string, string> = {
        // Documents
        'application/pdf': 'PDF-Dokument',
        'application/msword': 'Word-Dokument',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word-Dokument (.docx)',
        'application/vnd.oasis.opendocument.text': 'OpenDocument Text (.odt)',
        'application/vnd.ms-excel': 'Excel-Tabelle',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'Excel-Tabelle (.xlsx)',
        'application/vnd.oasis.opendocument.spreadsheet': 'OpenDocument Tabelle (.ods)',
        'application/vnd.ms-powerpoint': 'PowerPoint-Präsentation',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'PowerPoint-Präsentation (.pptx)',
        'application/vnd.oasis.opendocument.presentation': 'OpenDocument Präsentation (.odp)',
        'text/plain': 'Textdatei (.txt)',
        'text/rtf': 'RTF-Dokument',

        // Images
        'image/jpeg': 'JPEG-Bild',
        'image/png': 'PNG-Bild',
        'image/gif': 'GIF-Bild',
        'image/webp': 'WebP-Bild',
        'image/svg+xml': 'SVG-Grafik',
        'image/bmp': 'Bitmap-Bild',
        'image/tiff': 'TIFF-Bild',
        'image/heif': 'HEIF-Bild',
        'image/heic': 'HEIC-Bild',

        // Audio
        'audio/mpeg': 'MP3-Audiodatei',
        'audio/ogg': 'OGG-Audiodatei',
        'audio/wav': 'WAV-Audiodatei',
        'audio/flac': 'FLAC-Audiodatei',
        'audio/aac': 'AAC-Audiodatei',
        'audio/x-m4a': 'M4A-Audiodatei',

        // Video
        'video/mp4': 'MP4-Videodatei',
        'video/x-msvideo': 'AVI-Videodatei',
        'video/quicktime': 'MOV-Videodatei',
        'video/webm': 'WebM-Videodatei',
        'video/x-matroska': 'MKV-Videodatei',
        'video/mpeg': 'MPEG-Videodatei',

        // Code & Scripts
        'application/javascript': 'JavaScript-Datei',
        'application/json': 'JSON-Datei',
        'application/xml': 'XML-Datei',
        'text/html': 'HTML-Datei',
        'text/css': 'CSS-Datei',
        'text/javascript': 'JavaScript-Datei',
        'application/x-python': 'Python-Skript',
        'application/x-httpd-php': 'PHP-Datei',
        'application/x-sh': 'Shell-Skript',
        'application/sql': 'SQL-Datei',

        // Certificates & Security Files
        'application/x-pkcs12': 'PKCS#12 Zertifikat',
        'application/x-pem-file': 'PEM-Zertifikat',
        'application/x-x509-ca-cert': 'CA-Zertifikat',
        'application/x-pkcs7-certificates': 'PKCS#7-Zertifikatsdatei',
        'application/x-pkcs7-certreqresp': 'PKCS#7-Zertifikatsanforderung',

        // Archives & Compressed Files
        'application/zip': 'ZIP-Archiv',
        'application/x-rar-compressed': 'RAR-Archiv',
        'application/x-7z-compressed': '7z-Archiv',
        'application/gzip': 'GZIP-Datei',
        'application/x-tar': 'TAR-Archiv',

        // Database & Binary Files
        'application/octet-stream': 'Binärdatei',
        'application/x-sqlite3': 'SQLite-Datenbank',
        'application/x-msaccess': 'Access-Datenbank',
        'application/vnd.ms-cab-compressed': 'CAB-Archiv',
        'application/x-iso9660-image': 'ISO-Abbild',

        // Others
        'application/x-msdownload': 'Windows-Programm',
        'application/x-dosexec': 'DOS/Windows-Programm',
        'application/x-java-archive': 'Java-Archiv',
    };

    // Return file type label or a fallback
    return typeMap[contentType] || 'Unbekannte Datei';
}
