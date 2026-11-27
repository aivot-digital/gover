import {Box, Button, Grid, Typography, useTheme} from '@mui/material';
import {type FocusEvent, useCallback, useEffect, useRef, useState} from 'react';
import {BaseSummaryProps} from './base-summary';
import {RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';
import {MarkdownContent} from '../components/markdown-content/markdown-content';
import {useNormalizedReactId} from '../hooks/use-normalized-react-id';

/**
 * Long answers are limited by height instead of characters like text answers: cutting Markdown would show its syntax
 * and could cut a revealed link target. Browsers without the `lh` unit ignore the limit and show the whole answer.
 */
const COLLAPSED_MAX_HEIGHT = '5lh';

export function RichTextInputSummary(props: BaseSummaryProps<RichTextInputElement, string>) {
    const theme = useTheme();
    const contentId = `rich-text-summary-${useNormalizedReactId()}`;
    const contentRef = useRef<HTMLDivElement | null>(null);
    const [expanded, setExpanded] = useState(false);
    const [overflows, setOverflows] = useState(false);
    // Not trimmed: leading spaces change the Markdown, e.g. an indented code block.
    const markdown = props.value ?? '';
    const isEmpty = markdown.trim().length === 0;

    // Measured only while collapsed, because the expanded answer never overflows.
    const measureOverflow = useCallback(() => {
        const content = contentRef.current;
        if (content != null && !expanded) {
            setOverflows(content.scrollHeight > content.clientHeight + 1);
        }
    }, [expanded]);

    useEffect(() => {
        const content = contentRef.current;
        measureOverflow();
        if (content == null || typeof ResizeObserver === 'undefined') {
            return;
        }

        const observer = new ResizeObserver(measureOverflow);
        observer.observe(content);
        return () => observer.disconnect();
    }, [markdown, measureOverflow]);

    const isCollapsed = overflows && !expanded;

    // Keyboard focus must never rest on a link the collapsed answer hides.
    const handleFocus = (event: FocusEvent<HTMLDivElement>) => {
        const content = contentRef.current;
        if (isCollapsed && content != null && event.target.getBoundingClientRect().bottom > content.getBoundingClientRect().bottom) {
            setExpanded(true);
        }
    };

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid',
                borderBottomColor: 'divider',
                py: 1,
            }}
        >
            <Grid
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
                size={{
                    xs: 12,
                    md: 4,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                        [theme.breakpoints.up('md')]: {
                            fontWeight: 'normal',
                        },
                    }}
                >
                    {props.model.label}
                </Typography>
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    md: 8,
                }}
            >
                {isEmpty ? (
                    <Typography variant="body2">
                        Keine Angabe
                    </Typography>
                ) : (
                    <>
                        <Box
                            ref={contentRef}
                            id={contentId}
                            onFocus={handleFocus}
                            sx={{
                                typography: 'body2',
                                maxHeight: expanded ? 'none' : COLLAPSED_MAX_HEIGHT,
                                overflow: 'hidden',
                                // Fades out the last line, so the answer visibly continues.
                                maskImage: isCollapsed ? 'linear-gradient(to bottom, black 60%, transparent)' : undefined,
                            }}
                        >
                            {/* Rendered like the PDF of the submission, which prints link targets as well. Headings keep
                                the size of the summary text, so a heading in an answer does not look like a heading of
                                the summary. */}
                            <MarkdownContent
                                markdown={markdown}
                                revealLinkTargets
                                sx={{
                                    '& h1, & h2, & h3, & h4, & h5, & h6': {
                                        fontSize: 'inherit',
                                        fontWeight: 'bold',
                                        my: 1.5,
                                    },
                                }}
                            />
                        </Box>
                        {
                            overflows &&
                            <Box sx={{mt: 1}}>
                                <Button
                                    aria-expanded={expanded}
                                    aria-controls={contentId}
                                    onClick={() => setExpanded((current) => !current)}
                                >
                                    {expanded ? 'Weniger anzeigen' : 'Text vollständig anzeigen'}
                                </Button>
                            </Box>
                        }
                    </>
                )}
            </Grid>
        </Grid>
    );
}
