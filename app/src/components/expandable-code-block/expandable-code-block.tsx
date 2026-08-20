import React, {useId, useMemo, useState} from 'react';
import {alpha, Box, Button, type SxProps, type Theme, Typography, useTheme} from '@mui/material';
import {diffLines} from 'diff';
import {
    Highlight,
    type RenderProps,
} from 'prism-react-renderer';
import {createCodeSyntaxTheme, type CodeBlockLanguage} from './code-syntax-theme';

export type {CodeBlockLanguage} from './code-syntax-theme';

const MAX_DIFF_INPUT_LENGTH = 250_000;
const MAX_DIFF_EDIT_LENGTH = 2_000;
const MAX_DIFF_DURATION_MS = 100;

type DiffLineKind = 'unchanged' | 'added' | 'removed';
type DiffLineSource = 'current' | 'previous';

interface DiffLine {
    kind: DiffLineKind;
    source: DiffLineSource;
    sourceIndex: number;
}

interface DiffResult {
    lines: DiffLine[] | null;
    limitReached: boolean;
}

interface ExpandableCodeBlockProps {
    value: string;
    previousValue?: string;
    language?: CodeBlockLanguage;
    wrapLines?: boolean;
    sx?: SxProps<Theme>;
}

function createDiffResult(previousValue: string, value: string): DiffResult {
    // JsDiff runs synchronously in the browser. Keep pathological inputs from blocking the UI and fall back to the
    // highlighted current value when a useful comparison cannot be calculated within these bounds.
    if (previousValue.length + value.length > MAX_DIFF_INPUT_LENGTH) {
        return {
            lines: null,
            limitReached: true,
        };
    }

    const changes = diffLines(previousValue, value, {
        maxEditLength: MAX_DIFF_EDIT_LENGTH,
        timeout: MAX_DIFF_DURATION_MS,
    });
    if (changes == null) {
        return {
            lines: null,
            limitReached: true,
        };
    }

    const lines: DiffLine[] = [];
    let currentIndex = 0;
    let previousIndex = 0;

    for (const change of changes) {
        for (let index = 0; index < change.count; index++) {
            if (change.added) {
                lines.push({
                    kind: 'added',
                    source: 'current',
                    sourceIndex: currentIndex++,
                });
            } else if (change.removed) {
                lines.push({
                    kind: 'removed',
                    source: 'previous',
                    sourceIndex: previousIndex++,
                });
            } else {
                lines.push({
                    kind: 'unchanged',
                    source: 'current',
                    sourceIndex: currentIndex++,
                });
                previousIndex++;
            }
        }
    }

    // JsDiff returns no changes for two empty inputs, while Prism still renders one empty line.
    if (lines.length === 0) {
        lines.push({
            kind: 'unchanged',
            source: 'current',
            sourceIndex: 0,
        });
    }

    return {
        lines,
        limitReached: false,
    };
}

function getDiffMarker(kind: DiffLineKind): string {
    if (kind === 'added') {
        return '+';
    }
    if (kind === 'removed') {
        return '-';
    }
    return '';
}

function getDiffLineLabel(kind: DiffLineKind): string | undefined {
    if (kind === 'added') {
        return 'Hinzugefügte Zeile';
    }
    if (kind === 'removed') {
        return 'Entfernte Zeile';
    }
    return undefined;
}

export function ExpandableCodeBlock(props: ExpandableCodeBlockProps) {
    const {
        value,
        previousValue,
        language = 'text',
        wrapLines = false,
        sx,
    } = props;

    const theme = useTheme();
    const contentId = useId();
    const [expanded, setExpanded] = useState(false);
    const syntaxTheme = useMemo(() => createCodeSyntaxTheme(theme), [theme]);
    const diffResult = useMemo<DiffResult>(() => {
        return previousValue == null || previousValue === value
            ? {lines: null, limitReached: false}
            : createDiffResult(previousValue, value);
    }, [previousValue, value]);
    const diffDisplayLines = diffResult.lines;
    const lineCount = diffDisplayLines?.length ?? value.split('\n').length;
    const canToggle = lineCount > 20;
    const isCollapsed = canToggle && !expanded;
    const showFooter = canToggle || diffResult.limitReached;
    const collapsedFadeBackground = `linear-gradient(to bottom, ` +
        `${alpha(theme.palette.background.default, 0)} 0%, ` +
        `${alpha(theme.palette.background.default, 0.86)} 58%, ` +
        `${theme.palette.background.default} 88%)`;

    const renderLines = (current: RenderProps, previous: RenderProps): React.ReactNode => {
        const lines = diffDisplayLines ?? current.tokens.map((_, sourceIndex) => ({
            kind: 'unchanged' as const,
            source: 'current' as const,
            sourceIndex,
        }));

        return lines.map((line, renderedIndex) => {
            const source = line.source === 'previous' ? previous : current;
            const tokens = source.tokens[line.sourceIndex] ?? [];
            const lineProps = source.getLineProps({line: tokens});
            const isAdded = line.kind === 'added';
            const isRemoved = line.kind === 'removed';
            const diffLineLabel = getDiffLineLabel(line.kind);

            return (
                <Box
                    {...lineProps}
                    key={`${line.source}-${line.sourceIndex}-${renderedIndex}`}
                    component="span"
                    data-diff-kind={line.kind}
                    sx={{
                        display: 'flex',
                        minWidth: '100%',
                        width: wrapLines ? '100%' : 'max-content',
                        borderLeft: '3px solid',
                        borderLeftColor: isAdded
                            ? 'success.main'
                            : isRemoved
                                ? 'error.main'
                                : 'transparent',
                        backgroundColor: isAdded
                            ? alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.16 : 0.09)
                            : isRemoved
                                ? alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.16 : 0.08)
                                : 'transparent',
                    }}
                >
                    {diffLineLabel != null && (
                        <Box
                            component="span"
                            sx={{
                                position: 'absolute',
                                width: 1,
                                height: 1,
                                p: 0,
                                m: -1,
                                overflow: 'hidden',
                                clip: 'rect(0, 0, 0, 0)',
                                whiteSpace: 'nowrap',
                                border: 0,
                            }}
                        >
                            {diffLineLabel}:
                        </Box>
                    )}

                    {diffDisplayLines != null && (
                        <Box
                            component="span"
                            aria-hidden="true"
                            sx={{
                                width: '2.5ch',
                                flexShrink: 0,
                                color: isAdded
                                    ? 'success.main'
                                    : isRemoved
                                        ? 'error.main'
                                        : 'text.disabled',
                                textAlign: 'center',
                                userSelect: 'none',
                            }}
                        >
                            {getDiffMarker(line.kind)}
                        </Box>
                    )}

                    <Box component="span" sx={{pr: 1}}>
                        {tokens.map((token, tokenIndex) => (
                            <span
                                {...source.getTokenProps({token})}
                                key={tokenIndex}
                            />
                        ))}
                    </Box>
                </Box>
            );
        });
    };

    return (
        <Box
            data-testid="expandable-code-block"
            sx={[
                {
                    mb: 2,
                    overflow: 'hidden',
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    backgroundColor: 'background.default',
                    color: 'text.primary',
                    fontFamily: 'monospace',
                    fontSize: '0.875rem',
                    lineHeight: 1.5,
                    whiteSpace: wrapLines ? 'pre-wrap' : 'pre',
                    overflowWrap: wrapLines ? 'anywhere' : 'normal',
                    tabSize: 4,
                },
                ...(sx == null ? [] : Array.isArray(sx) ? sx : [sx]),
            ]}
        >
            <Box
                id={contentId}
                sx={{
                    position: 'relative',
                    maxHeight: isCollapsed ? 420 : 'none',
                    overflowY: isCollapsed ? 'hidden' : 'visible',
                }}
            >
                <Box
                    component="pre"
                    sx={{
                        m: 0,
                        p: 1,
                        overflowX: 'auto',
                        color: 'inherit',
                        font: 'inherit',
                        lineHeight: 'inherit',
                        whiteSpace: 'inherit',
                    }}
                >
                    <Box
                        component="code"
                        sx={{
                            display: 'block',
                            minWidth: wrapLines ? 0 : 'max-content',
                        }}
                    >
                        <Highlight
                            code={diffDisplayLines == null ? '' : previousValue ?? ''}
                            language={language}
                            theme={syntaxTheme}
                        >
                            {(previous) => (
                                <Highlight
                                    code={value}
                                    language={language}
                                    theme={syntaxTheme}
                                >
                                    {(current) => <>{renderLines(current, previous)}</>}
                                </Highlight>
                            )}
                        </Highlight>
                    </Box>
                </Box>

                {isCollapsed && (
                    <Box
                        sx={{
                            position: 'absolute',
                            right: 0,
                            bottom: 0,
                            left: 0,
                            height: 112,
                            background: collapsedFadeBackground,
                            pointerEvents: 'none',
                        }}
                    />
                )}
            </Box>

            {showFooter && (
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1,
                        justifyContent: 'flex-end',
                        px: 1,
                        py: 0.25,
                        borderTop: '1px solid',
                        borderColor: 'divider',
                    }}
                >
                    {diffResult.limitReached && (
                        <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{mr: 'auto', whiteSpace: 'normal'}}
                        >
                            Der Vergleich wurde wegen des Datenumfangs nicht dargestellt.
                        </Typography>
                    )}

                    {canToggle && (
                        <Button
                            variant="text"
                            size="small"
                            aria-controls={contentId}
                            aria-expanded={expanded}
                            onClick={() => setExpanded((current) => !current)}
                        >
                            {expanded ? 'Weniger anzeigen' : 'Vollständig anzeigen'}
                        </Button>
                    )}
                </Box>
            )}
        </Box>
    );
}
