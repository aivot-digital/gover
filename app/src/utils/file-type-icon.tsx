import React from 'react';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import AudiotrackOutlinedIcon from '@mui/icons-material/AudiotrackOutlined';
import VideoLibraryOutlinedIcon from '@mui/icons-material/VideoLibraryOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import CodeOutlinedIcon from '@mui/icons-material/CodeOutlined';
import TextSnippetOutlinedIcon from '@mui/icons-material/TextSnippetOutlined';
import StorageOutlinedIcon from '@mui/icons-material/StorageOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { SvgIconProps } from '@mui/material';

/**
 * Returns the appropriate icon for a given content type.
 * @param contentType - The MIME type of the file.
 * @param props - Optional: Additional properties for the icon (e.g., sx, className, etc.).
 * @returns A React element representing the corresponding icon.
 */
export function getFileTypeIcon(contentType: string, props?: SvgIconProps): JSX.Element {
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
    };

    // Retrieve the corresponding icon or use the default fallback icon
    const IconComponent = iconMap[contentType] || InsertDriveFileOutlinedIcon;

    // Create and return the React element with provided properties
    return React.createElement(IconComponent, props);
}
