import React from 'react';
import InsertDriveFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Draft';
import PictureAsPdfOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/PictureAsPdf';
import ImageOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Image';
import AudiotrackOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/MusicNote';
import VideoLibraryOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/VideoLibrary';
import DescriptionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import CodeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import TextSnippetOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/TextSnippet';
import StorageOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Storage';
import LockOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Lock';
import Folder from '@aivot/mui-material-symbols-400-n25-outlined/Folder';
import {type SvgIconProps} from '@mui/material';
import Csv from '@aivot/mui-material-symbols-400-n25-outlined/Csv';

const extensionContentTypeMap: Record<string, string> = {
    pdf: 'application/pdf',
    doc: 'application/msword',
    docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    xls: 'application/vnd.ms-excel',
    xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    ppt: 'application/vnd.ms-powerpoint',
    pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    txt: 'text/plain',
    csv: 'text/csv',
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    gif: 'image/gif',
    webp: 'image/webp',
    svg: 'image/svg+xml',
    bmp: 'image/bmp',
    tif: 'image/tiff',
    tiff: 'image/tiff',
    mp3: 'audio/mpeg',
    ogg: 'audio/ogg',
    wav: 'audio/wav',
    flac: 'audio/flac',
    mp4: 'video/mp4',
    avi: 'video/x-msvideo',
    mov: 'video/quicktime',
    webm: 'video/webm',
    mkv: 'video/x-matroska',
    js: 'application/javascript',
    mjs: 'application/javascript',
    json: 'application/json',
    xml: 'application/xml',
    html: 'text/html',
    htm: 'text/html',
    css: 'text/css',
    p12: 'application/x-pkcs12',
    pfx: 'application/x-pkcs12',
    pem: 'application/x-pem-file',
    sqlite: 'application/x-sqlite3',
    sqlite3: 'application/x-sqlite3',
};

function normalizeContentType(contentType: string): string {
    return contentType.split(';', 1)[0].trim().toLowerCase();
}

/**
 * Returns the appropriate icon for a given content type.
 * @param contentType - The MIME type of the file.
 * @param props - Optional: Additional properties for the icon (e.g., sx, className, etc.).
 * @returns A React element representing the corresponding icon.
 */
export function getFileTypeIcon(contentType: string, props?: SvgIconProps) {
    // Map of MIME types to corresponding Material UI icons
    const iconMap: Record<string, React.ElementType> = {
        // Documents
        'application/pdf': PictureAsPdfOutlinedIcon,
        'application/msword': DescriptionOutlinedIcon,
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': DescriptionOutlinedIcon,
        'application/vnd.ms-excel': DescriptionOutlinedIcon,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': DescriptionOutlinedIcon,
        'application/vnd.ms-powerpoint': DescriptionOutlinedIcon,
        'application/vnd.openxmlformats-officedocument.presentationml.presentation': DescriptionOutlinedIcon,
        'text/plain': TextSnippetOutlinedIcon,
        'text/csv': Csv,

        // Images
        'image/jpeg': ImageOutlinedIcon,
        'image/png': ImageOutlinedIcon,
        'image/gif': ImageOutlinedIcon,
        'image/webp': ImageOutlinedIcon,
        'image/svg+xml': ImageOutlinedIcon,
        'image/bmp': ImageOutlinedIcon,
        'image/tiff': ImageOutlinedIcon,

        // Audio
        'audio/mpeg': AudiotrackOutlinedIcon,
        'audio/ogg': AudiotrackOutlinedIcon,
        'audio/wav': AudiotrackOutlinedIcon,
        'audio/flac': AudiotrackOutlinedIcon,

        // Video
        'video/mp4': VideoLibraryOutlinedIcon,
        'video/x-msvideo': VideoLibraryOutlinedIcon,
        'video/quicktime': VideoLibraryOutlinedIcon,
        'video/webm': VideoLibraryOutlinedIcon,
        'video/x-matroska': VideoLibraryOutlinedIcon,

        // Code & Scripts
        'application/javascript': CodeOutlinedIcon,
        'application/json': CodeOutlinedIcon,
        'application/xml': CodeOutlinedIcon,
        'text/html': CodeOutlinedIcon,
        'text/css': CodeOutlinedIcon,
        'text/javascript': CodeOutlinedIcon,

        // Certificates & Security Files
        'application/x-pkcs12': LockOutlinedIcon,
        'application/x-pem-file': LockOutlinedIcon,
        'application/x-x509-ca-cert': LockOutlinedIcon,
        'application/x-pkcs7-certificates': LockOutlinedIcon,
        'application/x-pkcs7-certreqresp': LockOutlinedIcon,

        // Database & Binary Files
        'application/octet-stream': InsertDriveFileOutlinedIcon,
        'application/x-sqlite3': StorageOutlinedIcon,
        'application/x-msaccess': StorageOutlinedIcon,

        // Folders
        'inode/directory': Folder,
    };

    // Retrieve the corresponding icon or use the default fallback icon
    const IconComponent = iconMap[normalizeContentType(contentType)] || InsertDriveFileOutlinedIcon;

    // Create and return the React element with provided properties
    return React.createElement(IconComponent, props);
}

export function getFileTypeIconForFile(
    fileName: string,
    contentType?: string | null,
    props?: SvgIconProps,
) {
    const normalizedContentType = contentType == null ? '' : normalizeContentType(contentType);
    const extension = fileName.trim().toLowerCase().split('.').pop();
    const inferredContentType = extension == null ? undefined : extensionContentTypeMap[extension];
    const resolvedContentType = normalizedContentType.length > 0 && normalizedContentType !== 'application/octet-stream'
        ? normalizedContentType
        : inferredContentType ?? (normalizedContentType || 'application/octet-stream');

    return getFileTypeIcon(resolvedContentType, props);
}
