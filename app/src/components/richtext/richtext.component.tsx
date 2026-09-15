import React from 'react';
import {type SxProps, type Theme} from '@mui/material';
import {MarkdownContent} from '../markdown-content/markdown-content';

interface RichtextComponentProps {
    content?: string | null;
    className?: string;
    sx?: SxProps<Theme>;
}

export function RichtextComponent(props: RichtextComponentProps) {
    const {content, className, sx} = props;

    return (
        <MarkdownContent
            markdown={content}
            className={className}
            sx={[
                {
                    maxWidth: '660px',
                },
                ...(Array.isArray(sx) ? sx : [sx]),
            ]}
        />
    );
}
