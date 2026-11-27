import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {Button, type ButtonProps, type SxProps, type Theme} from '@mui/material';
import React, {type ReactNode} from 'react';

interface DocumentationLinkProps {
    url: string | null | undefined;
    size?: ButtonProps['size'];
    variant?: ButtonProps['variant'];
    sx?: SxProps<Theme>;
}

export function DocumentationLink(props: DocumentationLinkProps): ReactNode {
    if (props.url == null || props.url.trim().length === 0) {
        return null;
    }

    return (
        <Button
            component="a"
            href={props.url}
            target="_blank"
            rel="noopener noreferrer"
            size={props.size ?? 'small'}
            variant={props.variant ?? 'outlined'}
            endIcon={<OpenInNew/>}
            sx={props.sx}
        >
            Dokumentation öffnen
        </Button>
    );
}
