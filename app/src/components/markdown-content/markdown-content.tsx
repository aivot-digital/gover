import React from 'react';
import {Box, type SxProps, type Theme} from '@mui/material';
import {type Components} from 'react-markdown';
import {MarkdownRenderer} from './markdown-renderer';

interface MarkdownContentProps {
    markdown?: string | null;
    className?: string;
    sx?: SxProps<Theme>;
    components?: Components;
}

export function MarkdownContent(props: MarkdownContentProps) {
    const {markdown, className, sx, components} = props;

    return (
        <Box
            component="div"
            className={className}
            sx={[
                {
                    color: 'inherit',
                    wordBreak: 'break-word',
                    '& > :first-of-type': {
                        mt: 0,
                    },
                    '& > :last-child': {
                        mb: 0,
                    },
                    '& p': {
                        my: 1.5,
                    },
                    '& ul, & ol': {
                        my: 1.5,
                        pl: 3,
                    },
                    '& li + li': {
                        mt: 0.5,
                    },
                    '& a': {
                        color: 'primary.main',
                    },
                    '& code': {
                        fontFamily: 'Monaco, monospace',
                        color: 'text.primary',
                        backgroundColor: 'background.default',
                        overflowWrap: 'anywhere',
                        wordBreak: 'normal',
                        padding: '0.2em 0.4em',
                        borderRadius: '4px',
                    },
                    '& pre': {
                        my: 2,
                        p: 2,
                        overflowX: 'auto',
                        borderRadius: '4px',
                        color: 'text.primary',
                        backgroundColor: 'background.default',
                        whiteSpace: 'pre',
                        wordBreak: 'normal',
                        overflowWrap: 'normal',
                    },
                    '& pre code': {
                        padding: 0,
                        backgroundColor: 'transparent',
                        overflowWrap: 'normal',
                    },
                    '& blockquote': {
                        my: 2,
                        mx: 0,
                        pl: 2,
                        borderLeft: '4px solid',
                        borderColor: 'divider',
                        color: 'text.secondary',
                    },
                    '& table': {
                        width: '100%',
                        borderCollapse: 'collapse',
                        my: 2,
                    },
                    '& th, & td': {
                        border: '1px solid',
                        borderColor: 'divider',
                        p: 1,
                        textAlign: 'left',
                    },
                    '& .task-list-item': {
                        listStyle: 'none',
                        ml: -3,
                    }
                },
                ...(Array.isArray(sx) ? sx : [sx]),
            ]}
        >
            <MarkdownRenderer
                markdown={markdown}
                components={components}
            />
        </Box>
    );
}
