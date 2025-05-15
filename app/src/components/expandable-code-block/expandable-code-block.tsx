import React, { useState, useMemo } from 'react';
import { Box, Button } from '@mui/material';

function syntaxHighlight(jsonString: string): JSX.Element[] {
    const json = jsonString
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    const regex = /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g;

    const elements: JSX.Element[] = [];
    let lastIndex = 0;
    let match: RegExpExecArray | null;
    let index = 0;

    while ((match = regex.exec(json))) {
        const start = match.index;
        const end = regex.lastIndex;

        if (start > lastIndex) {
            elements.push(<span key={`t-${index++}`}>{json.slice(lastIndex, start)}</span>);
        }

        const token = match[0];
        let color = '#000';
        if (/^"/.test(token)) {
            if (/:$/.test(token)) {
                color = '#a71d5d'; // key
            } else {
                color = '#183691'; // string
            }
        } else if (/true|false/.test(token)) {
            color = '#0086b3'; // boolean
        } else if (/null/.test(token)) {
            color = '#b58900'; // null
        } else {
            color = '#008000'; // number
        }

        elements.push(
            <span key={`m-${index++}`} style={{ color }}>
                {token}
            </span>
        );

        lastIndex = end;
    }

    if (lastIndex < json.length) {
        elements.push(<span key={`e-${index++}`}>{json.slice(lastIndex)}</span>);
    }

    return elements;
}

export function ExpandableCodeBlock({ value }: { value: string }) {
    const [expanded, setExpanded] = useState(false);

    const lineCount = useMemo(() => {
        return value.split('\n').length;
    }, [value]);

    const canToggle = lineCount > 20;

    return (
        <Box sx={{ position: 'relative', mb: canToggle ? 4 : 2 }}>
            <Box
                sx={{
                    maxHeight: canToggle && !expanded ? 420 : 'none',
                    overflowY: canToggle && !expanded ? 'hidden' : 'visible',
                    overflowX: 'auto',
                    position: 'relative',
                    border: '1px solid #D6D6D7',
                    backgroundColor: '#fafafa',
                    borderRadius: '4px',
                    padding: 1,
                    fontFamily: 'monospace',
                    fontSize: '0.875rem',
                    whiteSpace: 'pre',
                    tabSize: 4,
                }}
            >
                {syntaxHighlight(value)}

                {canToggle && !expanded && (
                    <Box
                        sx={{
                            position: 'absolute',
                            bottom: 0,
                            left: 0,
                            right: 0,
                            height: 80,
                            background: 'linear-gradient(to bottom, rgba(250,250,250,0), rgba(250,250,250,1))',
                            pointerEvents: 'none',
                        }}
                    />
                )}
            </Box>

            {canToggle && (
                <Button
                    variant={"outlined"}
                    size="small"
                    onClick={() => setExpanded(!expanded)}
                    sx={{
                        position: 'absolute',
                        bottom: 24,
                        right: 24,
                        zIndex: 1,
                    }}
                >
                    {expanded ? 'Weniger anzeigen' : 'Mehr anzeigen'}
                </Button>
            )}
        </Box>
    );
}
