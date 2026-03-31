import React from 'react';
import {Box, type SxProps, type Theme} from '@mui/material';
import ReactMarkdown, {type Components} from 'react-markdown';
import remarkGfm from 'remark-gfm'

interface MarkdownContentProps {
    markdown?: string | null;
    className?: string;
    sx?: SxProps<Theme>;
    components?: Components;
}

const defaultComponents: Components = {
    a: ({href, ...props}) => {
        const isExternalLink = href != null && /^(https?:)?\/\//.test(href);

        return (
            <a
                href={href}
                {...props}
                target={isExternalLink ? '_blank' : undefined}
                rel={isExternalLink ? 'noopener noreferrer' : undefined}
            />
        );
    },
};

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
                        backgroundColor: '#f5f5f5',
                        padding: '0.2em 0.4em',
                        borderRadius: '4px',
                    },
                    '& pre': {
                        my: 2,
                        p: 2,
                        overflowX: 'auto',
                        borderRadius: '4px',
                        backgroundColor: '#f5f5f5',
                    },
                    '& pre code': {
                        padding: 0,
                        backgroundColor: 'transparent',
                    },
                    '& blockquote': {
                        my: 2,
                        mx: 0,
                        pl: 2,
                        borderLeft: '4px solid',
                        borderColor: 'divider',
                        color: 'text.secondary',
                    },
                    '& img': {
                        maxWidth: '100%',
                        height: 'auto',
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
                },
                ...(Array.isArray(sx) ? sx : [sx]),
            ]}
        >
            <ReactMarkdown
                components={{
                    ...defaultComponents,
                    ...components,
                }}
                remarkPlugins={[
                    remarkGfm,
                ]}
            >
                {markdown ?? ''}
            </ReactMarkdown>
        </Box>
    );
}
