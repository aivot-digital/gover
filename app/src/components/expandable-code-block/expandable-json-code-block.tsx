import React, {useMemo, useState} from 'react';
import {Box, Button, SxProps} from '@mui/material';

const INDENT = '    ';

type RenderLineKind = 'default' | 'changed' | 'previous';

interface RenderLine {
    text: string;
    kind: RenderLineKind;
}

interface SyntaxHighlightColors {
    text: string;
    key: string;
    string: string;
    boolean: string;
    null: string;
    number: string;
}

const DEFAULT_SYNTAX_HIGHLIGHT_COLORS: SyntaxHighlightColors = {
    text: '#000',
    key: '#a71d5d',
    string: '#183691',
    boolean: '#0086b3',
    null: '#b58900',
    number: '#008000',
};

const PREVIOUS_SYNTAX_HIGHLIGHT_COLORS: SyntaxHighlightColors = {
    text: '#8b1e1e',
    key: '#9f1239',
    string: '#b42318',
    boolean: '#c2410c',
    null: '#a16207',
    number: '#9a3412',
};

function syntaxHighlight(jsonString: string, tone: 'default' | 'previous' = 'default'): React.ReactNode[] {
    const regex = /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g;
    const colors = tone === 'previous'
        ? PREVIOUS_SYNTAX_HIGHLIGHT_COLORS
        : DEFAULT_SYNTAX_HIGHLIGHT_COLORS;

    const elements: React.ReactNode[] = [];
    let lastIndex = 0;
    let match: RegExpExecArray | null;
    let index = 0;

    while ((match = regex.exec(jsonString))) {
        const start = match.index;
        const end = regex.lastIndex;

        if (start > lastIndex) {
            elements.push(
                <span key={`t-${index++}`} style={{color: colors.text}}>
                    {jsonString.slice(lastIndex, start)}
                </span>
            );
        }

        const token = match[0];
        let color = colors.text;
        if (/^"/.test(token)) {
            if (/:$/.test(token)) {
                color = colors.key;
            } else {
                color = colors.string;
            }
        } else if (/true|false/.test(token)) {
            color = colors.boolean;
        } else if (/null/.test(token)) {
            color = colors.null;
        } else {
            color = colors.number;
        }

        elements.push(
            <span key={`m-${index++}`} style={{ color }}>
                {token}
            </span>
        );

        lastIndex = end;
    }

    if (lastIndex < jsonString.length) {
        elements.push(
            <span key={`e-${index++}`} style={{color: colors.text}}>
                {jsonString.slice(lastIndex)}
            </span>
        );
    }

    return elements;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return Object.prototype.toString.call(value) === '[object Object]';
}

function stringifyJsonValue(value: unknown): string {
    if (value === undefined) {
        return 'undefined';
    }

    const stringifiedValue = JSON.stringify(value);
    return stringifiedValue ?? String(value);
}

function createRenderLine(text: string, kind: RenderLineKind = 'default'): RenderLine {
    return {
        text,
        kind,
    };
}

function canRecurseObjectDiff(value: Record<string, unknown>, diff: Record<string, unknown>): boolean {
    return Object.keys(diff).every((key) => Object.prototype.hasOwnProperty.call(value, key));
}

function canRecurseArrayDiff(value: unknown[], diff: unknown[]): boolean {
    return Object.keys(diff).every((key) => {
        const index = Number(key);

        return Number.isInteger(index)
            && index >= 0
            && index < value.length
            && Object.prototype.hasOwnProperty.call(diff, index);
    });
}

function buildPlainValueLines(
    value: unknown,
    indentLevel: number,
    prefix: string,
    isLast: boolean,
    kind: RenderLineKind,
): RenderLine[] {
    let indent = INDENT.repeat(indentLevel);
    if (kind !== 'default') {
        indent = indent.substring(1);
    }
    const trailingComma = isLast ? '' : ',';

    if (isPlainObject(value)) {
        const entries = Object.entries(value);
        if (entries.length === 0) {
            return [createRenderLine(`${indent}${prefix}{}${trailingComma}`, kind)];
        }

        const lines = [createRenderLine(`${indent}${prefix}{`, kind)];

        entries.forEach(([key, childValue], index) => {
            lines.push(
                ...buildPlainValueLines(
                    childValue,
                    indentLevel + 1,
                    `${JSON.stringify(key)}: `,
                    index === entries.length - 1,
                    kind,
                )
            );
        });

        lines.push(createRenderLine(`${indent}}${trailingComma}`, kind));
        return lines;
    }

    if (Array.isArray(value)) {
        if (value.length === 0) {
            return [createRenderLine(`${indent}${prefix}[]${trailingComma}`, kind)];
        }

        const lines = [createRenderLine(`${indent}${prefix}[`, kind)];

        value.forEach((item, index) => {
            lines.push(
                ...buildPlainValueLines(
                    item,
                    indentLevel + 1,
                    '',
                    index === value.length - 1,
                    kind,
                )
            );
        });

        lines.push(createRenderLine(`${indent}]${trailingComma}`, kind));
        return lines;
    }

    return [createRenderLine(`${indent}${prefix}${stringifyJsonValue(value)}${trailingComma}`, kind)];
}

function buildCurrentValueLines(
    value: unknown,
    diff: unknown,
    indentLevel: number,
    prefix: string,
    isLast: boolean,
): RenderLine[] {
    if (isPlainObject(value) && isPlainObject(diff) && canRecurseObjectDiff(value, diff)) {
        const entries = Object.entries(value);
        const indent = INDENT.repeat(indentLevel);
        const trailingComma = isLast ? '' : ',';

        if (entries.length === 0) {
            return [createRenderLine(`${indent}${prefix}{}${trailingComma}`)];
        }

        const lines = [createRenderLine(`${indent}${prefix}{`)];

        entries.forEach(([key, childValue], index) => {
            const childDiff = Object.prototype.hasOwnProperty.call(diff, key)
                ? diff[key]
                : undefined;

            lines.push(
                ...buildCurrentValueLines(
                    childValue,
                    childDiff,
                    indentLevel + 1,
                    `${JSON.stringify(key)}: `,
                    index === entries.length - 1,
                )
            );
        });

        lines.push(createRenderLine(`${indent}}${trailingComma}`));
        return lines;
    }

    if (Array.isArray(value) && Array.isArray(diff) && canRecurseArrayDiff(value, diff)) {
        const indent = INDENT.repeat(indentLevel);
        const trailingComma = isLast ? '' : ',';

        if (value.length === 0) {
            return [createRenderLine(`${indent}${prefix}[]${trailingComma}`)];
        }

        const lines = [createRenderLine(`${indent}${prefix}[`)];

        value.forEach((item, index) => {
            const childDiff = Object.prototype.hasOwnProperty.call(diff, index)
                ? diff[index]
                : undefined;

            lines.push(
                ...buildCurrentValueLines(
                    item,
                    childDiff,
                    indentLevel + 1,
                    '',
                    index === value.length - 1,
                )
            );
        });

        lines.push(createRenderLine(`${indent}]${trailingComma}`));
        return lines;
    }

    const hasDiff = diff !== undefined;
    const lines = buildPlainValueLines(value, indentLevel, prefix, isLast, hasDiff ? 'changed' : 'default');

    if (hasDiff) {
        lines.push(...buildPlainValueLines(diff, indentLevel, prefix, true, 'previous'));
    }

    return lines;
}

function buildRenderLines(value: unknown, diff: unknown): RenderLine[] {
    return buildCurrentValueLines(value, diff, 0, '', true);
}

interface ExpandableJSONCodeBlockProps {
    value: Record<string, unknown>;
    diff?: Record<string, unknown>;
    sx?: SxProps;
}

export function ExpandableJSONCodeBlock(props: ExpandableJSONCodeBlockProps) {
    const {
        value: oValue,
        diff,
        sx,
    } = props;

    const lines = useMemo(() => {
        return buildRenderLines(oValue, diff);
    }, [oValue, diff]);

    const [expanded, setExpanded] = useState(false);

    const lineCount = lines.length;
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
                    lineHeight: 1.5,
                    tabSize: 4,
                    ...sx,
                }}
            >
                {lines.map((line, index) => (
                    <Box
                        key={`json-line-${index}`}
                        component="div"
                        sx={{
                            display: 'block',
                            whiteSpace: 'pre',
                            ...(line.kind === 'changed' ? {
                                backgroundColor: '#eef5ff',
                                borderLeft: '3px solid #1976d2',
                                pl: 1,
                            } : {}),
                            ...(line.kind === 'previous' ? {
                                backgroundColor: '#fff1f1',
                                borderLeft: '3px solid #d32f2f',
                                color: '#8b1e1e',
                                pl: 1,
                            } : {}),
                        }}
                    >
                        {syntaxHighlight(line.text, line.kind === 'previous' ? 'previous' : 'default')}
                    </Box>
                ))}

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
